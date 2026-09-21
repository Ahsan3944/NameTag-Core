package com.ultraop.nametag.core.model;
import org.junit.jupiter.api.Test;
import java.util.List; import java.util.Map; import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class PlayerAssignmentTest {
 @Test void expirationIsStoredPerAssignedTag(){TagId vip=new TagId("vip"); PlayerAssignment a=new PlayerAssignment(UUID.randomUUID(),List.of(new TagId("owner"),vip),new TagId("owner"),Map.of(vip,2000L)); assertFalse(a.isExpired(vip,1999L)); assertTrue(a.isExpired(vip,2000L));}
 @Test void expirationMustReferToAssignedTag(){assertThrows(IllegalArgumentException.class,()->new PlayerAssignment(UUID.randomUUID(),List.of(new TagId("owner")),null,Map.of(new TagId("vip"),2000L)));}
 @Test void legacyConstructorHasNoExpiration(){assertFalse(new PlayerAssignment(UUID.randomUUID(),List.of(new TagId("owner")),new TagId("owner")).hasExpirations());}
}
