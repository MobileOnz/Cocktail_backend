package com.application.domain.member.entity;

import com.application.common.time.BaseTimeEntity;
import com.application.domain.member.enums.AgeRange;
import com.application.domain.member.enums.Gender;
import com.application.domain.member.enums.Role;
import com.application.domain.member.enums.SocialLogin;
import com.application.domain.monitoring.entity.Monitoring;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Getter
@Setter
@RequiredArgsConstructor
@Entity(name="member")
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="credential_id", nullable = false, unique = true)
    private String credentialId;

    @Column(name="name")
    private String name;

    @Column(name="nickname")
    private String nickname;

    @Column(name="email")
    private String email;

    @Column(name="social_login")
    private String socialLogin;

    @Column(name="gender")
    private Gender gender;

    @Column(name="addr")
    private String addr;

    @Column(name="age")
    private Integer age;

    @Column(name="age_range")
    private AgeRange ageRange;

    @Column(name="profile")
    private String profile;

    @Column(name="phone")
    private String phone;

    @Column(name="role", nullable = false)
    private Role role;// Admin, User, Other

    @Column(name="age_term", nullable = false)
    private Boolean ageTerm;

    @Column(name="service_term", nullable = false)
    private Boolean serviceTerm;

    @Column(name="marketing_term")
    private Boolean marketingTerm;

    @Column(name="ad_term")
    private Boolean adTerm;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<Monitoring> monitorings = new ArrayList<>();

    @Builder
    public Member(String credentialId, String name, String nickname, String email, SocialLogin socialLogin, String profile, Role role
    ,Boolean ageTerm, Boolean serviceTerm, Boolean marketingTerm, Boolean adTerm){
        this.credentialId = credentialId;
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.socialLogin = socialLogin.toString();
        this.profile = profile;
        this.role = role;
        this.ageTerm = ageTerm;
        this.serviceTerm = serviceTerm;
        this.marketingTerm = marketingTerm;
        this.adTerm = adTerm;
    }

    public String getRole(){
        return this.role.getEnglish();
    }
}
