//package com.cyoda.presto.client.treenode;
//
//import com.cyoda.plugins.ValueMaps;
//import com.cyoda.presto.client.treenode.dto.DataRequestDto;
//import com.cyoda.presto.client.treenode.dto.EntityContentDto;
//import com.cyoda.presto.client.treenode.dto.schema.FieldConfigDto;
//import com.cyoda.presto.client.treenode.dto.schema.SchemaConfigDto;
//import com.cyoda.presto.client.treenode.dto.schema.TableConfigDto;
//import com.cyoda.presto.client.types.DataType;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//import java.util.Random;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//public class TreeNodeTrinoAPIMock {
//    private static final UUID profileClassId = UUID.randomUUID();
//    private static final UUID productClassId = UUID.randomUUID();
//    private static final UUID orgClassId = UUID.randomUUID();
//    private static final Map<String, DataType> profileTypeRefs = profileMockTypeRefs();
//    private static final Map<String, DataType> productTypeRefs = productMockTypeRefs();
//    private static final Map<String, DataType> orgTypeRefs = orgMockTypeRefs();
//
//    private static final Map<UUID, List<EntityContentDto>> generatedData = generateData();
//
//    private static Map<String, DataType> orgMockTypeRefs() {
//        Map<String, DataType> typeReferences = new HashMap<>();
//        typeReferences.put("organization.name", DataType.STRING);
//        typeReferences.put("organization.type", DataType.STRING); // e.g., Non-profit, Corporation, etc.
//
//        typeReferences.put("organization.address.street", DataType.STRING);
//        typeReferences.put("organization.address.city", DataType.STRING);
//        typeReferences.put("organization.address.state", DataType.STRING);
//        typeReferences.put("organization.address.zipCode", DataType.STRING);
//        typeReferences.put("organization.contact.email", DataType.STRING);
//        typeReferences.put("organization.contact.phone", DataType.STRING);
//
//        typeReferences.put("organization.employeeCount", DataType.INTEGER);
//        typeReferences.put("organization.foundingDate", DataType.LOCAL_DATE);
//        return typeReferences;
//    }
//    private static Map<String, DataType> productMockTypeRefs() {
//        Map<String, DataType> typeReferences = new HashMap<>();
//        typeReferences.put("product.externalId", DataType.UUID_TYPE);
//        typeReferences.put("product.name", DataType.STRING);
//        typeReferences.put("product.description", DataType.STRING);
//        typeReferences.put("product.category", DataType.STRING);
//        typeReferences.put("product.price", DataType.BIG_DECIMAL);
//        typeReferences.put("product.currency", DataType.STRING); // Assuming no direct DataType for currency
//        typeReferences.put("product.stockQuantity", DataType.INTEGER);
//        typeReferences.put("product.availability", DataType.BOOLEAN);
//        typeReferences.put("product.creationDate", DataType.LOCAL_DATE);
//        typeReferences.put("product.lastUpdateDateTime", DataType.LOCAL_DATE_TIME);
//        typeReferences.put("product.dimensions.length", DataType.DOUBLE);
//        typeReferences.put("product.dimensions.width", DataType.DOUBLE);
//        typeReferences.put("product.dimensions.height", DataType.DOUBLE);
//        typeReferences.put("product.weight", DataType.DOUBLE);
//        typeReferences.put("product.tags", DataType.STRING); // Could be an array of strings, but simplified here
//        typeReferences.put("product.rating", DataType.DOUBLE);
//        return typeReferences;
//    }
//    private static Map<String, DataType> profileMockTypeRefs(){
//        Map<String, DataType> typeReferences = new HashMap<>();
//
//        typeReferences.put("profile.name", DataType.STRING);
//        typeReferences.put("profile.age", DataType.INTEGER);
//        typeReferences.put("profile.email", DataType.STRING);
//        typeReferences.put("profile.birthDate", DataType.LOCAL_DATE);
//        typeReferences.put("profile.signupDateTime", DataType.LOCAL_DATE_TIME);
//        typeReferences.put("profile.lastLoginTime", DataType.LOCAL_TIME);
//        typeReferences.put("profile.isActive", DataType.BOOLEAN);
//        typeReferences.put("profile.balance", DataType.BIG_DECIMAL);
//        typeReferences.put("profile.heightInMeters", DataType.FLOAT);
//        typeReferences.put("profile.weight", DataType.DOUBLE);
//        typeReferences.put("profile.numberOfLogins", DataType.LONG);
//        typeReferences.put("profile.preferences.language", DataType.LOCALE);
//        typeReferences.put("profile.preferences.timezone", DataType.STRING); // Assuming no direct DataType for timezone
//        typeReferences.put("profile.address.country", DataType.STRING);
//        typeReferences.put("profile.address.city", DataType.STRING);
//        typeReferences.put("profile.address.postalCode", DataType.INTEGER);
//        typeReferences.put("profile.verificationStatus", DataType.BOOLEAN);
//        typeReferences.put("profile.uuid", DataType.UUID_TYPE);
//        return typeReferences;
//    }
//    public List<SchemaConfigDto> getSchemas(String userId){
//        return Collections.singletonList(
//                new SchemaConfigDto("tree_node_schema", //maybe hashmap
//                        List.of(new TableConfigDto("profiles", profileClassId,
//                                null, null) //fields are filled at other point
//                                , new TableConfigDto("products", productClassId, null, null),
//                                new TableConfigDto("orgs", orgClassId, null, null)))
//        );
//    }
//    private FieldConfigDto fromTypeRef(String name, DataType type){
//        String trinoFieldName = name.substring(name.lastIndexOf(".")+1);
//        return new FieldConfigDto(trinoFieldName, trinoFieldName, name, type.toString());
//    }
//    public TableConfigDto getMetadata(UUID metadataClassId){
//        if (metadataClassId.equals(profileClassId))
//            return new TableConfigDto(null, profileClassId, null,
//                profileTypeRefs.entrySet().stream().map((e)-> fromTypeRef(e.getKey(), e.getValue()))
//                        .collect(Collectors.toList()));
//        if (metadataClassId.equals(productClassId))
//            return new TableConfigDto(null, productClassId, null,
//                    productTypeRefs.entrySet().stream().map((e)-> fromTypeRef(e.getKey(), e.getValue()))
//                            .collect(Collectors.toList()));
//        if (metadataClassId.equals(orgClassId))
//            return new TableConfigDto(null, orgClassId, null,
//                    orgTypeRefs.entrySet().stream().map((e)-> fromTypeRef(e.getKey(), e.getValue()))
//                            .collect(Collectors.toList()));
//        return null;
//    }
//
//    public Iterable<EntityContentDto> getData(DataRequestDto dataRequest){
//
//        return generatedData.get(dataRequest.getMetaClassId());
//    }
//
//    private static Map<UUID, List<EntityContentDto>> generateData(){
//        Map<UUID, List<EntityContentDto>> result = new HashMap<>();
//        UUID[] profileIds = new UUID[10];
//        UUID[] orgIds = new UUID[5];
//        List<EntityContentDto> orgs = new ArrayList<>();
//        for (int i = 0; i < 5; i++) {
//            UUID orgId = UUID.randomUUID();
//            orgIds[i] = orgId;
//            ValueMaps vm = new ValueMaps();
//            populateValueMapsSimple(vm, orgTypeRefs);
//            EntityContentDto contentDto = new EntityContentDto(orgId, null, new Date(), vm.consolidateMaps());
//            orgs.add(contentDto);
//        }
//        result.put(orgClassId, orgs);
//        List<EntityContentDto> profiles = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            UUID profileId = UUID.randomUUID();
//            profileIds[i] = profileId;
//            ValueMaps vm = new ValueMaps();
//            populateValueMapsSimple(vm, profileTypeRefs);
//            EntityContentDto contentDto = new EntityContentDto(profileId, orgIds[i/2], new Date(), vm.consolidateMaps());
//            profiles.add(contentDto);
//        }
//        result.put(profileClassId, profiles);
//        List<EntityContentDto> products = new ArrayList<>();
//        for (int i = 0; i < 50; i++) {
//            ValueMaps vm = new ValueMaps();
//            populateValueMapsSimple(vm, productTypeRefs);
//            EntityContentDto contentDto = new EntityContentDto(UUID.randomUUID(),
//                    profileIds[i/5], new Date(), vm.consolidateMaps());
//            products.add(contentDto);
//        }
//        result.put(productClassId, products);
//        return result;
//    }
//
//    private static void populateValueMapsSimple(ValueMaps valueMaps, Map<String, DataType> typeReferences) {
//        Random random = new Random();
//        typeReferences.forEach((path, type) -> {
//            switch (type) {
//                case STRING -> valueMaps.getStrings().put(path, "Sample String for " + path);
//                case INTEGER -> valueMaps.getInts().put(path, random.nextInt());
//                case LOCAL_DATE -> valueMaps.getLocalDates().put(path, LocalDate.now());
//                case LOCAL_DATE_TIME -> valueMaps.getLocalDateTimes().put(path, LocalDateTime.now());
//                case LOCAL_TIME -> valueMaps.getLocalTimes().put(path, LocalTime.now());
////                case LOCAL_DATE ->
////                        valueMaps.getLocalDates().put(path, LocalDate.now().minusDays(random.nextInt(365 * 2)));
////                case LOCAL_DATE_TIME ->
////                        valueMaps.getLocalDateTimes().put(path, LocalDateTime.now().minusHours(random.nextInt(24 * 30)));
////                case LOCAL_TIME ->
////                        valueMaps.getLocalTimes().put(path, LocalTime.now().minusMinutes(random.nextInt(60)));
//                case BOOLEAN -> valueMaps.getBooleans().put(path, random.nextBoolean());
//                case BIG_DECIMAL ->
//                        valueMaps.getBigDecimals().put(path, new BigDecimal(Double.toString(random.nextDouble() * 10000)).setScale(2, BigDecimal.ROUND_HALF_UP));
//                case FLOAT -> valueMaps.getFloats().put(path, random.nextFloat() * 100);
//                case DOUBLE -> valueMaps.getDoubles().put(path, random.nextDouble() * 100);
//                case LONG -> valueMaps.getLongs().put(path, random.nextLong());
//                case LOCALE -> {
//                    Locale[] availableLocales = Locale.getAvailableLocales();
//                    valueMaps.getLocales().put(path, availableLocales[random.nextInt(availableLocales.length)]);
//                }
//                case UUID_TYPE -> valueMaps.getUuids().put(path, UUID.randomUUID());
//
////                case INTEGER -> valueMaps.getInts().put(path, 42);
////                case LOCAL_DATE -> valueMaps.getLocalDates().put(path, LocalDate.now());
////                case LOCAL_DATE_TIME -> valueMaps.getLocalDateTimes().put(path, LocalDateTime.now());
////                case LOCAL_TIME -> valueMaps.getLocalTimes().put(path, LocalTime.now());
////                case BOOLEAN -> valueMaps.getBooleans().put(path, true);
////                case BIG_DECIMAL -> valueMaps.getBigDecimals().put(path, new BigDecimal("123.45"));
////                case FLOAT -> valueMaps.getFloats().put(path, 12.34f);
////                case DOUBLE -> valueMaps.getDoubles().put(path, 123.456);
////                case LONG -> valueMaps.getLongs().put(path, 123456789L);
////                case LOCALE -> valueMaps.getLocales().put(path, Locale.US);
////                case UUID_TYPE -> valueMaps.getUuids().put(path, UUID.randomUUID());
//
//            }
//        });
//    }
//}
