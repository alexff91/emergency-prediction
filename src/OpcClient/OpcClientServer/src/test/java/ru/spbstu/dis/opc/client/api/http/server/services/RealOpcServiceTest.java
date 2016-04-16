package ru.spbstu.dis.opc.client.api.http.server.services;

import com.google.inject.Guice;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import org.junit.Ignore;
import org.junit.Test;
import ru.spbstu.dis.opc.client.api.http.server.guice.OpcWrapperGuiceModule;
import java.util.Set;

@Ignore
public class RealOpcServiceTest {
  @Test
  public void testName()
  throws Exception {
    final RealOpcService opcService = Guice.createInjector(new OpcWrapperGuiceModule())
        .getInstance(RealOpcService.class);
    final Set<String> tags = opcService.tags();
    assertThat(tags).isNotEmpty();
    final String tagNameToCheck = "MixingConnection/M/TP_2M5";
    assertThat(tags).contains(tagNameToCheck);
    final Boolean currentTagValue = opcService.readBoolean(tagNameToCheck);
    assertThat(currentTagValue).isNotNull();
    opcService.writeValueForTag(tagNameToCheck, false);
  }
}