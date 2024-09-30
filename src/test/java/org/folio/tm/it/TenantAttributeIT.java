package org.folio.tm.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.tm.service.ServiceUtils.example;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import org.assertj.core.api.Condition;
import org.folio.test.types.IntegrationTest;
import org.folio.tm.base.BaseIntegrationTest;
import org.folio.tm.domain.dto.TenantAttribute;
import org.folio.tm.domain.dto.TenantAttributes;
import org.folio.tm.domain.entity.TenantAttributeEntity;
import org.folio.tm.domain.entity.TenantEntity;
import org.folio.tm.repository.TenantAttributeRepository;
import org.folio.tm.support.TestConstants;
import org.folio.tm.support.TestUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@IntegrationTest
@Sql(scripts = {
  "classpath:/sql/populate_tenants.sql",
  "classpath:/sql/populate_tenant_attrs.sql"
})
@SqlMergeMode(MERGE)
@Sql(scripts = "classpath:/sql/clear_tenants.sql", executionPhase = AFTER_TEST_METHOD)
public class TenantAttributeIT extends BaseIntegrationTest {

  private static final UUID ATTRIBUTE_ID_1 = UUID.fromString("1d0a1cb6-0250-40a2-8408-644b51252ee8");
  private static final UUID ATTRIBUTE_ID_2 = UUID.fromString("ae1b9f2b-6a1c-410d-aafd-2c8227ecf4e0");

  private static final TenantAttribute ATTRIBUTE1 = new TenantAttribute()
      .id(ATTRIBUTE_ID_1)
      .key("key1")
      .value("value1");

  private static final TenantAttribute ATTRIBUTE11 = new TenantAttribute()
      .id(ATTRIBUTE_ID_2)
      .key("key11")
      .value("value11");

  @Autowired
  private TenantAttributeRepository repository;

  @Test
  void get_positive() throws Exception {
    doGet("/tenants/{tenantId}/tenant-attributes/{id}", TestConstants.TENANT_ID, ATTRIBUTE1.getId())
      .andExpect(json("tenant-attr/get-tenant-attr-response.json"));
  }

  @Test
  void get_negative_notFoundById() throws Exception {
    UUID attrId = UUID.randomUUID();

    attemptGet("/tenants/{tenantId}/tenant-attributes/{id}", TestConstants.TENANT_ID, attrId)
      .andExpectAll(notFoundWithMsg(
        "Tenant attribute is not found: id = " + attrId + ", tenantId = " + TestConstants.TENANT_ID));
  }

  @Test
  void get_negative_notFoundByTenantId() throws Exception {
    UUID tenantId = UUID.randomUUID();

    attemptGet("/tenants/{tenantId}/tenant-attributes/{id}", tenantId, ATTRIBUTE1.getId())
      .andExpectAll(notFoundWithMsg(
        "Tenant attribute is not found: id = " + ATTRIBUTE1.getId() + ", tenantId = " + tenantId));
  }

  @Test
  void get_all_positive() throws Exception {
    doGet("/tenants/{tenantId}/tenant-attributes", TestConstants.TENANT_ID)
      .andExpect(json("tenant-attr/get-all-tenant-attrs-response.json"));
  }

  @Test
  @Sql(scripts = "classpath:/sql/clear_tenant_attrs.sql")
  void getAll_positive_noAttrs() throws Exception {
    doGet("/tenants/{tenantId}/tenant-attributes", TestConstants.TENANT_ID)
      .andExpectAll(emptyCollection("tenantAttributes"));
  }

  @Test
  void getAll_positive_offsetAndLimit() throws Exception {
    doGet(MockMvcRequestBuilders.get("/tenants/{tenantId}/tenant-attributes", TestConstants.TENANT_ID)
        .queryParam("offset", "9").queryParam("limit", "1"))
      .andExpect(json("tenant-attr/get-all-tenant-attrs-offset-limit-response.json"));
  }

  @Test
  void getByQuery_positive() throws Exception {
    doGet(MockMvcRequestBuilders.get("/tenants/{tenantId}/tenant-attributes", TestConstants.TENANT_ID)
        .queryParam("query", "key==\"key1\" or key==\"key2\""))
      .andExpect(json("tenant-attr/get-tenant-attrs-by-query-response.json"));
  }

  @Test
  @Sql("classpath:/sql/clear_tenant_attrs.sql")
  void createAll_positive_newCollection() throws Exception {
    var attr1 = copyFrom(ATTRIBUTE1).id(null);
    var attr11 = copyFrom(ATTRIBUTE11).id(null);
    var attrs = new TenantAttributes().tenantAttributes(List.of(attr1, attr11));

    var mvcResult = doPost("/tenants/{tenantId}/tenant-attributes", attrs, TestConstants.TENANT_ID).andReturn();
    var resp = TestUtils.parseResponse(mvcResult, TenantAttributes.class);

    assertThat(resp.getTotalRecords()).isEqualTo(attrs.getTenantAttributes().size());
    assertThat(resp.getTenantAttributes())
      .haveExactly(1, keyAndValueAs(attr1))
      .haveExactly(1, keyAndValueAs(attr11));

    var saved = repository.findAll(tenantIdExample());
    assertThat(saved).hasSize(attrs.getTenantAttributes().size());
  }

  @Test
  void createAll_positive_mergeCollection() throws Exception {
    var attr1 = copyFrom(ATTRIBUTE1).value("modified");
    var attr11 = copyFrom(ATTRIBUTE11);
    var attrs = new TenantAttributes().tenantAttributes(List.of(attr1, attr11));

    var mvcResult = doPost("/tenants/{tenantId}/tenant-attributes", attrs, TestConstants.TENANT_ID).andReturn();
    var resp = TestUtils.parseResponse(mvcResult, TenantAttributes.class);

    assertThat(resp.getTotalRecords()).isEqualTo(attrs.getTenantAttributes().size());
    assertThat(resp.getTenantAttributes())
      .haveExactly(1, sameAs(attr1))
      .haveExactly(1, sameAs(attr11));

    var saved = repository.findAll(tenantIdExample());
    assertThat(saved).hasSize(attrs.getTenantAttributes().size());
  }

  @Test
  void createAll_positive_emptyCollection() throws Exception {
    var attrs = new TenantAttributes();

    doPost("/tenants/{tenantId}/tenant-attributes", attrs, TestConstants.TENANT_ID)
      .andExpectAll(emptyCollection("tenantAttributes"));

    var saved = repository.findAll(tenantIdExample());
    assertThat(saved).isEmpty();
  }

  @Test
  void update_positive() throws Exception {
    var attr = copyFrom(ATTRIBUTE1).value("modified");

    doPut("/tenants/{tenantId}/tenant-attributes/{id}", attr, TestConstants.TENANT_ID, attr.getId())
      .andExpect(jsonPath("$.value", is(attr.getValue())));

    var saved = repository.findById(ATTRIBUTE_ID_1).orElseThrow();
    assertThat(saved.getValue()).isEqualTo(attr.getValue());
  }

  @Test
  void update_negative_notFound() throws Exception {
    var uuid = UUID.randomUUID();

    attemptPut("/tenants/{tenantId}/tenant-attributes/{id}", ATTRIBUTE1, TestConstants.TENANT_ID, uuid)
      .andExpectAll(notFoundWithMsg("Tenant attribute is not found: id = " + uuid
        + ", tenantId = " + TestConstants.TENANT_ID));
  }

  @Test
  void update_negative_idDoesntMatch() throws Exception {
    var attr = copyFrom(ATTRIBUTE1).id(UUID.randomUUID());

    attemptPut("/tenants/{tenantId}/tenant-attributes/{id}", attr, TestConstants.TENANT_ID, ATTRIBUTE1.getId())
      .andExpectAll(requestValidationErr("Tenant attribute id doesn't match to the one in the path",
          "id", attr.getId()));
  }

  @Test
  void update_negative_emptyKey() throws Exception {
    var attr = copyFrom(ATTRIBUTE1).key(null);

    attemptPut("/tenants/{tenantId}/tenant-attributes/{id}", attr, TestConstants.TENANT_ID, attr.getId())
      .andExpectAll(argumentNotValidErr("must not be null", "key", null));
  }

  @Test
  void update_negative_emptyValue() throws Exception {
    var attr = copyFrom(ATTRIBUTE1).value(null);

    attemptPut("/tenants/{tenantId}/tenant-attributes/{id}", attr, TestConstants.TENANT_ID, attr.getId())
      .andExpectAll(argumentNotValidErr("must not be null", "value", null));
  }

  @Test
  void update_negative_duplicateKey() throws Exception {
    var attr = copyFrom(ATTRIBUTE1).key("key2");

    attemptPut("/tenants/{tenantId}/tenant-attributes/{id}", attr, TestConstants.TENANT_ID, attr.getId())
      .andExpectAll(dataIntegrityErr("duplicate key value violates unique constraint \"unq_tenant_attr_tenant_key\""));
  }

  @Test
  void delete_positive() throws Exception {
    var existing = repository.findById(ATTRIBUTE_ID_1);
    assertTrue(existing.isPresent());

    doDelete("/tenants/{tenantId}/tenant-attributes/{id}", TestConstants.TENANT_ID, ATTRIBUTE1.getId());

    repository.findById(ATTRIBUTE_ID_1)
      .ifPresent(tenantEntity -> Assertions.fail("Tenant attribute is not deleted: " + ATTRIBUTE1.getId()));
  }

  @Test
  void delete_positive_notPresent() throws Exception {
    UUID id = UUID.randomUUID();
    var existing = repository.findById(id);
    assertFalse(existing.isPresent());

    doDelete("/tenants/{tenantId}/tenant-attributes/{id}", TestConstants.TENANT_ID, id);
  }

  private static TenantAttribute copyFrom(TenantAttribute source) {
    return new TenantAttribute()
      .id(source.getId())
      .key(source.getKey())
      .value(source.getValue());
  }

  @NotNull
  private static Condition<TenantAttribute> keyAndValueAs(TenantAttribute attr) {
    return new Condition<>(ta ->
      Objects.nonNull(ta.getId())
        && Objects.equals(ta.getKey(), attr.getKey())
        && Objects.equals(ta.getValue(), attr.getValue())
        && Objects.nonNull(ta.getMetadata()),
      "Expected but no found: %s", attr);
  }

  private static Condition<TenantAttribute> sameAs(TenantAttribute attr) {
    return new Condition<>(ta ->
      Objects.equals(ta.getId(), attr.getId())
        && Objects.equals(ta.getKey(), attr.getKey())
        && Objects.equals(ta.getValue(), attr.getValue())
        && Objects.nonNull(ta.getMetadata()),
      "Expected but no found: %s", attr);
  }

  private static Example<TenantAttributeEntity> tenantIdExample() {
    return example(TenantAttributeEntity::new, setTenantId());
  }

  private static Consumer<TenantAttributeEntity> setTenantId() {
    return ta -> ta.setTenant(TenantEntity.of(TestConstants.TENANT_ID));
  }
}
