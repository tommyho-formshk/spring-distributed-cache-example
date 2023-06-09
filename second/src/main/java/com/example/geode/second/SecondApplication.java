package com.example.geode.second;

import com.example.geode.common.dto.Customer;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.wan.GatewayReceiver;
import org.apache.geode.cache.wan.GatewaySender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.gemfire.PeerRegionFactoryBean;
import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.*;
import org.springframework.data.gemfire.util.ArrayUtils;
import org.springframework.data.gemfire.util.RegionUtils;
import org.springframework.data.gemfire.wan.GatewayReceiverFactoryBean;
import org.springframework.data.gemfire.wan.GatewaySenderFactoryBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
@SuppressWarnings("unused")
public class SecondApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecondApplication.class, args);
	}

	private static final boolean PERSISTENT = false;

	private static final int GATEWAY_RECEIVER_END_PORT = 29779;
	private static final int GATEWAY_RECEIVER_START_PORT = 13339;

	private static final String CUSTOMERS_BY_NAME_REGION = "CustomersByName";
	private static final String GATEWAY_RECEIVER_HOSTNAME_FOR_SENDERS = "localhost";


	@CacheServerApplication(name = "SecondApplication")
	static class GeodeServerConfiguration {

		@Bean(CUSTOMERS_BY_NAME_REGION)
		ReplicatedRegionFactoryBean<String, Customer> customersByNameRegion(Cache cache,
																			@Autowired(required = false) List<RegionConfigurer> regionConfigurers) {

			ReplicatedRegionFactoryBean<String, Customer> customersByName = new ReplicatedRegionFactoryBean<>();

			customersByName.setCache(cache);
			customersByName.setPersistent(PERSISTENT);
			customersByName.setRegionConfigurers(regionConfigurers);

			return customersByName;
		}

		@Bean
		ApplicationRunner geodeClusterObjectsBootstrappedAssertionRunner(Environment environment, Cache cache,
																		 Region<?, ?> customersByName, GatewayReceiver gatewayReceiver, GatewaySender gatewaySender) {

			return args -> {

				assertThat(cache).isNotNull();
				assertThat(cache.getName()).startsWith(SecondApplication.class.getSimpleName());
				assertThat(customersByName).isNotNull();
				assertThat(customersByName.getAttributes()).isNotNull();
				assertThat(customersByName.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.REPLICATE);
				assertThat(customersByName.getAttributes().getGatewaySenderIds()).containsExactly(gatewaySender.getId());
				assertThat(customersByName.getName()).isEqualTo(CUSTOMERS_BY_NAME_REGION);
				assertThat(customersByName.getRegionService()).isEqualTo(cache);
				assertThat(cache.getRegion(RegionUtils.toRegionPath(CUSTOMERS_BY_NAME_REGION))).isEqualTo(customersByName);
				assertThat(gatewayReceiver).isNotNull();
				assertThat(gatewayReceiver.isRunning()).isTrue();
				assertThat(cache.getGatewayReceivers()).containsExactly(gatewayReceiver);
				assertThat(gatewaySender).isNotNull();
				assertThat(gatewaySender.isRunning()).isTrue();
				assertThat(cache.getGatewaySenders().stream().map(GatewaySender::getId).collect(Collectors.toSet()))
						.containsExactly(gatewaySender.getId());

				System.err.printf("Apache Geode Cluster [%s] configured and bootstrapped successfully!%n",
						environment.getProperty("spring.application.name", "UNKNOWN"));
			};
		}
	}

	@Configuration
	@EnableLocator
	@EnableManager(start = true)
	@Profile("locator-manager")
	static class GeodeLocatorManagerConfiguration { }

	@Configuration
	@Profile("gateway-receiver")
	static class GeodeGatewayReceiverConfiguration {

		@Bean
		GatewayReceiverFactoryBean gatewayReceiver(Cache cache) {

			GatewayReceiverFactoryBean gatewayReceiver = new GatewayReceiverFactoryBean(cache);

			gatewayReceiver.setHostnameForSenders(GATEWAY_RECEIVER_HOSTNAME_FOR_SENDERS);
			gatewayReceiver.setStartPort(GATEWAY_RECEIVER_START_PORT);
			gatewayReceiver.setEndPort(GATEWAY_RECEIVER_END_PORT);

			return gatewayReceiver;
		}
	}

	@Configuration
	@Profile("gateway-sender")
	static class GeodeGatewaySenderConfiguration {

		@Bean
		GatewaySenderFactoryBean customersByNameGatewaySender(Cache cache,
															  @Value("${geode.distributed-system.remote.id:1}") int remoteDistributedSystemId) {

			GatewaySenderFactoryBean gatewaySender = new GatewaySenderFactoryBean(cache);

			gatewaySender.setPersistent(PERSISTENT);
			gatewaySender.setRemoteDistributedSystemId(remoteDistributedSystemId);

			return gatewaySender;
		}

		@Bean
		RegionConfigurer customersByNameConfigurer(GatewaySender gatewaySender) {

			return new RegionConfigurer() {

				@Override
				public void configure(String beanName, PeerRegionFactoryBean<?, ?> regionBean) {

					if (CUSTOMERS_BY_NAME_REGION.equals(beanName)) {
						regionBean.setGatewaySenders(ArrayUtils.asArray(gatewaySender));
					}
				}
			};
		}
	}

}
