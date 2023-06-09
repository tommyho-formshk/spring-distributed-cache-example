package com.example.geode.common.dto;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.gemfire.ScopeType;
import org.springframework.data.gemfire.mapping.annotation.Region;
import org.springframework.data.gemfire.mapping.annotation.ReplicateRegion;

@Region(name="Customers")
@EqualsAndHashCode
@ToString(of = "name")
@RequiredArgsConstructor(staticName = "newCustomer")
public class Customer implements java.io.Serializable {

    @Id
    @Getter
    @NonNull
    private Long id;

    @Getter @NonNull
    private String name;

    public Customer() {
    }
}