package com.example.rms.user.infrastructure;
import com.example.rms.shared.domain.*;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import org.springframework.core.env.Environment;
record BootstrapConfiguration(String username,String email,String displayName,String password) {
    private static class EmailField { @Email String value; }
    static BootstrapConfiguration validated(Environment env,Validator validator) {
        String username=field(env,"RMS_BOOTSTRAP_USERNAME",64);
        String email=field(env,"RMS_BOOTSTRAP_EMAIL",254);
        if(!validator.validateValue(EmailField.class,"value",email).isEmpty()) throw invalid("RMS_BOOTSTRAP_EMAIL");
        String display=field(env,"RMS_BOOTSTRAP_DISPLAY_NAME",100);
        String password=env.getProperty("RMS_BOOTSTRAP_PASSWORD");
        try { PasswordInputPolicy.validate(password); } catch(RmsException e) { throw invalid("RMS_BOOTSTRAP_PASSWORD"); }
        return new BootstrapConfiguration(username,email,display,password);
    }
    private static String field(Environment env,String name,int max) {
        try { return InputPolicy.nonBlank(env.getProperty(name),max); } catch(RmsException e) { throw invalid(name); }
    }
    private static IllegalStateException invalid(String field) { return new IllegalStateException("BOOTSTRAP_CONFIG_INVALID:"+field); }
    @Override public String toString() { return "BootstrapConfiguration[REDACTED]"; }
}
