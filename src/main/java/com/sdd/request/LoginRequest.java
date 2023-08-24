package com.sdd.request;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString


@Data
public class LoginRequest {
    String userId;
    String password;


}
