package com.example.geode.second;

import com.example.geode.common.dto.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private Environment environment;

    @GetMapping("/customers/{name}")
    public CustomerHolder searchBy(@PathVariable String name) throws InterruptedException {

        return CustomerHolder.from(this.customerService.findBy(name))
                .setCacheMiss(this.customerService.isCacheMiss());
    }

    @GetMapping("/ping")
    public String pingPong() {
        return "PONG";
    }

    @GetMapping("/")
    public String home() {

        return String.format("%s is running!",
                environment.getProperty("spring.application.name", "UNKNOWN"));
    }

    @GetMapping("/clear/{region}")
    public String clean(@PathVariable String region) {
        customerService.removeCache(region);
        return "cache were cleaned";
    }

    @GetMapping("/list/{region}")
    public Collection<Object> list(@PathVariable String region) {
        return customerService.listCacheByRegion(region);
    }

    public static class CustomerHolder {

        public static CustomerHolder from(Customer customer) {
            return new CustomerHolder(customer);
        }

        private boolean cacheMiss = true;

        private final Customer customer;

        protected CustomerHolder(Customer customer) {

            Assert.notNull(customer, "Customer must not be null");

            this.customer = customer;
        }

        public CustomerHolder setCacheMiss(boolean cacheMiss) {
            this.cacheMiss = cacheMiss;
            return this;

        }

        public boolean isCacheMiss() {
            return this.cacheMiss;
        }

        public Customer getCustomer() {
            return customer;
        }
    }
}