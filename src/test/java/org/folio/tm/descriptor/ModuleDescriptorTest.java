package org.folio.tm.descriptor;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.tm.support.TestUtils.OBJECT_MAPPER;
import static org.folio.tm.support.TestUtils.readString;

import java.util.List;
import java.util.Set;
import org.folio.common.domain.model.InterfaceDescriptor;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.common.domain.model.Permission;
import org.folio.common.domain.model.RoutingEntry;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * Verifies that the internal module descriptor is structurally valid and ready to be bundled into the
 * {@code app-mgr-services} platform application (MGRTENANT-107).
 *
 * <p>The descriptor is loaded from the same classpath location and into the same model that
 * {@code org.folio.security.service.InternalModuleDescriptorProvider} uses at startup, so a failure here is a real
 * startup failure.</p>
 */
@UnitTest
class ModuleDescriptorTest {

  private static final String DESCRIPTOR_PATH = "descriptors/ModuleDescriptor.json";

  private static JsonNode descriptorJson;
  private static ModuleDescriptor descriptor;

  @BeforeAll
  static void beforeAll() {
    var json = readString(DESCRIPTOR_PATH);
    descriptorJson = OBJECT_MAPPER.readTree(json);
    descriptor = OBJECT_MAPPER.readValue(json, ModuleDescriptor.class);
  }

  @Test
  void moduleDescriptor_positive_parsesIntoSharedModel() {
    assertThat(descriptor.getId()).isNotBlank();
    assertThat(descriptor.getDescription()).isNotBlank();
    assertThat(descriptor.getName()).isEqualTo("mgr-tenants");
    assertThat(descriptor.getVersion()).isNotBlank();
    assertThat(descriptor.isMgrComponent()).isTrue();
    assertThat(descriptor.getProvides()).isNotEmpty();
    assertThat(descriptor.getPermissionSets()).isNotEmpty();
  }

  @Test
  void moduleDescriptor_positive_handlersAreFullyDefined() {
    assertThat(handlers()).isNotEmpty().allSatisfy(handler -> {
      assertThat(handler.getMethods()).isNotEmpty();
      assertThat(handler.getPathPattern()).isNotBlank();
      assertThat(handler.getPermissionsRequired()).isNotEmpty();
    });

    assertThat(descriptor.getProvides()).allSatisfy(provided -> {
      assertThat(provided.getId()).isNotBlank();
      assertThat(provided.getVersion()).isNotBlank();
    });
  }

  @Test
  void moduleDescriptor_positive_allHandlerPermissionsDefined() {
    var definedPermissions = definedPermissionNames();

    var requiredPermissions = handlers().stream()
      .flatMap(handler -> handler.getPermissionsRequired().stream())
      .collect(toSet());

    assertThat(requiredPermissions).isNotEmpty().allSatisfy(permission ->
      assertThat(definedPermissions).as("permission required by a handler must be defined in permissionSets: %s",
        permission).contains(permission));
  }

  @Test
  void moduleDescriptor_positive_subPermissionsResolve() {
    var definedPermissions = definedPermissionNames();

    var subPermissions = descriptor.getPermissionSets().stream()
      .flatMap(permission -> permission.getSubPermissions().stream())
      .collect(toSet());

    assertThat(subPermissions).isNotEmpty().allSatisfy(permission ->
      assertThat(definedPermissions).as("subPermission must be defined in permissionSets: %s", permission)
        .contains(permission));
  }

  @Test
  void moduleDescriptor_positive_permissionNamesUnique() {
    var permissionNames = descriptor.getPermissionSets().stream()
      .map(Permission::getPermissionName)
      .toList();

    assertThat(permissionNames).isNotEmpty().doesNotHaveDuplicates().allSatisfy(name ->
      assertThat(name).isNotBlank());
  }

  /**
   * mgr-tenants uses Kafka only through the admin client, to delete {@code {ENV}.{tenant}.*} topics on tenant
   * deletion. It neither produces nor consumes Kafka events, so the descriptor declares no events (MGRTENANT-107).
   *
   * <p>Asserted on the raw JSON tree rather than on {@link ModuleDescriptor}, because the model has no event fields
   * and the test object mapper silently ignores unknown properties.</p>
   */
  @Test
  void moduleDescriptor_negative_noKafkaEventsDeclared() {
    assertThat(descriptorJson.has("producesEvents"))
      .as("mgr-tenants publishes no Kafka events; see MGRTENANT-107 before declaring producesEvents")
      .isFalse();

    assertThat(descriptorJson.has("consumesEvents"))
      .as("mgr-tenants consumes no Kafka events; see MGRTENANT-107 before declaring consumesEvents")
      .isFalse();
  }

  private static Set<String> definedPermissionNames() {
    return descriptor.getPermissionSets().stream()
      .map(Permission::getPermissionName)
      .collect(toSet());
  }

  private static List<RoutingEntry> handlers() {
    return descriptor.getProvides().stream()
      .map(InterfaceDescriptor::getHandlers)
      .flatMap(List::stream)
      .toList();
  }
}
