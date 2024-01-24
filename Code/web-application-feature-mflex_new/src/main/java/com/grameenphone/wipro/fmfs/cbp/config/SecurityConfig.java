package com.grameenphone.wipro.fmfs.cbp.config;

import com.grameenphone.wipro.fmfs.cbp.Application;
import com.grameenphone.wipro.fmfs.cbp.filter.SAMLLoginFilter;
import com.grameenphone.wipro.fmfs.cbp.filter.SAMLLogoutFilter;
import com.grameenphone.wipro.fmfs.cbp.service.AuthService;
import com.grameenphone.wipro.utility.LookupUtil;
import com.grameenphone.wipro.utility.security.CryptoUtil;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * configures spring security
 */
@Configuration
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfig  {
    @Autowired
    @Lazy
    AuthService authService;

    @Bean
    public GrantedAuthorityDefaults authorityDefaults() {
    	return new GrantedAuthorityDefaults("");
    }

    @Value("${saml.idp.entity_id}")
    String idpEntityId;

    @Value("${saml.idp.web_sso_url}")
    String idpWebSSOUrl;

    @Value("${saml.idp.logout_url}")
    String idpLogoutUrl;

    @Value("${saml.sp.callback}")
    String spCallBackUrl;

    @Value("${saml.sp.entity_id}")
    String spEntityId;

    @Value("${saml.sp.base_url}")
    String spBaseUrl;

    String autoLoginId = Application.environment.getProperty("auto.login"); //could not be autowired as this property is optional

    public static final String logoutUrl = "/api/saml/logout";
    public static final String loginUrl = "/api/saml/login";

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/favicon.ico", "/static/**", "/ng/**");
    }
    /**
     * disables csrf and configures saml2 authentication
     * @param http
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.addFilterBefore(new SAMLLoginFilter(configureOneLogin(), spCallBackUrl, authService, autoLoginId), AnonymousAuthenticationFilter.class).addFilterBefore(new SAMLLogoutFilter(configureOneLogin(), spCallBackUrl, authService, autoLoginId), SAMLLoginFilter.class).csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
        
    @Bean
    public Saml2Settings configureOneLogin() throws MalformedURLException, URISyntaxException, CertificateException, FileNotFoundException {
        Security.addProvider(new BouncyCastleProvider());
        X509Certificate idpCertificate = CryptoUtil.loadCertificate(LookupUtil.lookupConfFile("idp_cert.crt").getAbsolutePath());
        Map<String, Object> samlData = new HashMap<>();
        samlData.put("onelogin.saml2.sp.entityid", spEntityId);
        samlData.put("onelogin.saml2.sp.assertion_consumer_service.url", new URI(spBaseUrl + loginUrl).toURL());
        samlData.put("onelogin.saml2.security.want_xml_validation", false);
        samlData.put("onelogin.saml2.sp.x509cert", idpCertificate);

        samlData.put("onelogin.saml2.idp.entityid", idpEntityId);
        samlData.put("onelogin.saml2.idp.single_sign_on_service.url", idpWebSSOUrl);
        samlData.put("onelogin.saml2.idp.single_sign_on_service.binding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect");
        samlData.put("onelogin.saml2.idp.single_logout_service.url", idpLogoutUrl);
        samlData.put("onelogin.saml2.idp.single_logout_service.response.url", idpLogoutUrl);
        samlData.put("onelogin.saml2.idp.single_logout_service.binding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect");
        samlData.put("onelogin.saml2.idp.x509cert", idpCertificate);

        SettingsBuilder builder = new SettingsBuilder();
        return builder.fromValues(samlData).build();
    }
}