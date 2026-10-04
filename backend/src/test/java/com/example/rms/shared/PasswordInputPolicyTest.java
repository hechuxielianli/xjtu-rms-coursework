package com.example.rms.shared;
import com.example.rms.shared.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
class PasswordInputPolicyTest {
    @Test void utf8BoundaryUsesOriginalValue() {
        for(String accepted:new String[]{"a","a".repeat(72),"汉".repeat(24),"😀".repeat(18),"  value  ","e\u0301"})assertSame(accepted,PasswordInputPolicy.validate(accepted));
        for(String denied:new String[]{""," ","\t\n","a".repeat(73),"汉".repeat(25),"😀".repeat(19)})assertEquals(ErrorCode.INVALID_INPUT,assertThrows(RmsException.class,()->PasswordInputPolicy.validate(denied)).code());
        assertThrows(RmsException.class,()->PasswordInputPolicy.validate(null));
        var bcrypt=new BCryptPasswordEncoder();String hash=bcrypt.encode(PasswordInputPolicy.validate("  value  "));
        assertTrue(bcrypt.matches("  value  ",hash));assertFalse(bcrypt.matches("value",hash));
        String composed=bcrypt.encode(PasswordInputPolicy.validate("é"));assertFalse(bcrypt.matches("e\u0301",composed));
    }
    @Test void bigintRangeIsExactAndCanonical() {
        assertEquals(Long.MAX_VALUE,InputPolicy.decimalId("9223372036854775807"));assertEquals(0L,InputPolicy.lockVersion("0"));
        for(String invalid:new String[]{"0","01","-1","1.0","9223372036854775808","1e3"})assertThrows(RmsException.class,()->InputPolicy.decimalId(invalid));
    }
}
