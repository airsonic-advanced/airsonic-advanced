# airsonic

## Summary

| Metric | Value |
|--------|-------|
| Total Issues | 80 |
| Mandatory Blockers | 26 |
| Potential Issues | 29 |

## Component Information

| Property | Value |
|----------|-------|
| Language | Java, JavaScript |
| Frameworks | Spring Boot, Spring, JakartaEE |
| Build tools | Maven |
| JDK version | 21 |

## Cloud Readiness Issues

| Issue Name | Criticality | Story Points | Occurrences |
|------------|-------------|--------------|-------------|
| Use of unsecured network protocols or URI libraries | Mandatory | 3 | [319](#Use_of_unsecured_network_protocols_or_URI_libraries) |
| File system - Java NIO | Mandatory | 3 | [145](#File_system_-_Java_NIO) |
| CRA: Default or well-known password detected | Mandatory | 3 | [53](#CRA_Default_or_well-known_password_detected) |
| Hardcoded IP Address | Mandatory | 3 | [30](#Hardcoded_IP_Address) |
| Caching - Spring Boot Cache library | Mandatory | 5 | [16](#Caching_-_Spring_Boot_Cache_library) |
| File system - Java IO | Mandatory | 3 | [10](#File_system_-_Java_IO) |
| Local HTTP Calls | Mandatory | 3 | [10](#Local_HTTP_Calls) |
| File system - java.net.URL/URI | Mandatory | 3 | [8](#File_system_-_java_net_URL_URI) |
| CRA: Hard-coded password in Java source code | Mandatory | 8 | [8](#CRA_Hard-coded_password_in_Java_source_code) |
| Java Native Processes | Mandatory | 8 | [7](#Java_Native_Processes) |
| File system - 'file://' scheme | Mandatory | 3 | [5](#File_system_-_file_scheme) |
| CRA: Use of weak hash algorithm MD5 | Mandatory | 5 | [4](#CRA_Use_of_weak_hash_algorithm_MD5) |
| Apache Commons I/O | Mandatory | 3 | [3](#Apache_Commons_I_O) |
| CRA: Insecure TLS cipher suites | Mandatory | 5 | [3](#CRA_Insecure_TLS_cipher_suites) |
| CRA: Use of insecure random number generator java.util.Random | Mandatory | 5 | [2](#CRA_Use_of_insecure_random_number_generator_java_util_Random) |
| CRA: Use of weak password hashing (plain MD5/SHA for passwords) | Mandatory | 8 | [2](#CRA_Use_of_weak_password_hashing_plain_MD5_SHA_for_passwords) |
| CRA: Hard-coded API key or secret in Java source code | Mandatory | 8 | [2](#CRA_Hard-coded_API_key_or_secret_in_Java_source_code) |
| CRA: Use of weak hash algorithm SHA-1 | Mandatory | 5 | [1](#CRA_Use_of_weak_hash_algorithm_SHA-1) |
| Avoid File System Logging in Configuration | Mandatory | 1 | [1](#Avoid_File_System_Logging_in_Configuration) |
| Caching - Ehcache embedded library | Mandatory | 5 | [1](#Caching_-_Ehcache_embedded_library) |
| Java Mail API | Potential | 5 | [6](#Java_Mail_API) |
| The java.net.URLEncoder.encode method uses UTF-8 by default | Potential | 3 | [3](#The_java_net_URLEncoder_encode_method_uses_UTF-8_by_default) |
| Embedded framework - Spring Security | Potential | 5 | [2](#Embedded_framework_-_Spring_Security) |
| MySQL database found | Potential | 5 | [2](#MySQL_database_found) |
| PostgreSQL database found | Potential | 5 | [2](#PostgreSQL_database_found) |
| The 'java.io' constructor defaults to UTF-8 | Potential | 3 | [2](#The_java_io_constructor_defaults_to_UTF-8) |
| The java.net.URLDecoder.decode method uses UTF-8 by default | Potential | 3 | [2](#The_java_net_URLDecoder_decode_method_uses_UTF-8_by_default) |
| Password found in configuration file | Potential | 3 | [1](#Password_found_in_configuration_file) |
| Java Swing usage | Potential | 5 | [1](#Java_Swing_usage) |
| Avoid using hardcoded URLs (HTTP protocol) in source code | Optional | 3 | [571](#Avoid_using_hardcoded_URLs_HTTP_protocol_in_source_code) |
| Java LDAP usage detected | Optional | 8 | [11](#Java_LDAP_usage_detected) |
| Environment variables/system properties | Optional | 3 | [11](#Environment_variables_system_properties) |
| Avoid using hardcoded URLs (LDAP protocol) in source code | Optional | 3 | [2](#Avoid_using_hardcoded_URLs_LDAP_protocol_in_source_code) |
| Socket communication | Optional | 8 | [1](#Socket_communication) |

### Issue Details

<details id="Use_of_unsecured_network_protocols_or_URI_libraries">
<summary><b>Use of unsecured network protocols or URI libraries</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/ajax/LyricsWSController.java (line 78)`
- `airsonic-main/src/main/java/org/airsonic/player/service/sonos/SonosServiceRegistration.java (line 95)`
- `airsonic-main/src/main/java/org/airsonic/player/service/sonos/SonosLinkSecurityInterceptor.java (line 142)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 700)`
- `airsonic-main/src/main/java/org/airsonic/player/util/XMLUtil.java (line 9)`
- `airsonic-main/src/main/java/org/airsonic/player/util/XMLUtil.java (line 10)`
- `airsonic-main/src/main/java/org/airsonic/player/util/XMLUtil.java (line 11)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 698)`
- `airsonic-main/src/test/java/org/airsonic/player/service/JWTSecurityServiceTest.java (line 31)`
- `airsonic-main/src/test/java/org/airsonic/player/service/PlaylistServiceTestImport.java (line 156)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 475)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 475)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 376)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 474)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 641)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 702)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 475)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 700)`
- `integration-test/src/test/java/org/airsonic/test/Scanner.java (line 35)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 25)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 26)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 27)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 28)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 29)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 30)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 31)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 32)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 33)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 34)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 35)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 37)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 38)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 43)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 45)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 33)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 43)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 45)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 46)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 47)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 50)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 53)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 58)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 62)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 65)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 80)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 97)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 111)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 148)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 153)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 175)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 195)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 206)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 307)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 309)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 330)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 452)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 464)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 575)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 829)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 907)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 909)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 248)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 250)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 376)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 474)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 647)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 705)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 700)`
- `airsonic-main/src/test/java/org/airsonic/player/util/StringUtilTestCase.java (line 141)`
- `airsonic-main/src/test/java/org/airsonic/player/util/StringUtilTestCase.java (line 142)`
- `airsonic-main/src/test/java/org/airsonic/player/util/StringUtilTestCase.java (line 143)`
- `airsonic-main/src/test/java/org/airsonic/player/util/StringUtilTestCase.java (line 144)`
- `airsonic-main/src/test/java/org/airsonic/player/util/StringUtilTestCase.java (line 146)`
- `airsonic-main/src/test/java/org/airsonic/player/util/StringUtilTestCase.java (line 147)`
- `airsonic-main/src/test/java/org/airsonic/player/util/StringUtilTestCase.java (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 307)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 309)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 464)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 575)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 828)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 906)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PodcastService.java (line 101)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PodcastService.java (line 102)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 242)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 244)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 466)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 623)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 680)`
- `airsonic-main/src/main/java/org/airsonic/player/ajax/CoverArtWSController.java (line 17)`
- `airsonic-main/src/main/java/org/airsonic/player/ajax/LyricsWSController.java (line 31)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/ProxyController.java (line 28)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PodcastService.java (line 51)`
- `airsonic-main/src/main/java/org/airsonic/player/service/VersionService.java (line 33)`
- `airsonic-main/src/main/java/org/airsonic/player/service/podcast/PodcastIndexService.java (line 16)`
- `airsonic-main/src/main/java/org/airsonic/player/service/scrobbler/LastFMScrobbler.java (line 35)`
- `airsonic-main/src/main/java/org/airsonic/player/service/scrobbler/ListenBrainzScrobbler.java (line 31)`
- `airsonic-main/src/main/java/org/airsonic/player/service/sonos/SonosServiceRegistration.java (line 31)`
- `airsonic-main/src/main/java/org/airsonic/player/ajax/CoverArtWSController.java (line 16)`
- `airsonic-main/src/main/java/org/airsonic/player/ajax/LyricsWSController.java (line 30)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/ProxyController.java (line 27)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PodcastService.java (line 50)`
- `airsonic-main/src/main/java/org/airsonic/player/service/VersionService.java (line 32)`
- `airsonic-main/src/main/java/org/airsonic/player/service/podcast/PodcastIndexService.java (line 15)`
- `airsonic-main/src/main/java/org/airsonic/player/service/scrobbler/LastFMScrobbler.java (line 34)`
- `airsonic-main/src/main/java/org/airsonic/player/service/scrobbler/ListenBrainzScrobbler.java (line 30)`
- `airsonic-main/src/main/java/org/airsonic/player/service/sonos/SonosServiceRegistration.java (line 30)`

</details>

<details id="File_system_-_Java_NIO">
<summary><b>File system - Java NIO</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/controller/UploadController.java (line 65)`
- `airsonic-main/src/main/java/org/airsonic/player/ajax/CoverArtWSController.java (line 28)`
- `airsonic-main/src/main/java/org/airsonic/player/ajax/CoverArtWSController.java (line 27)`
- `airsonic-main/src/main/java/org/airsonic/player/ajax/CoverArtWSController.java (line 29)`
- `airsonic-main/src/main/java/org/airsonic/player/command/MusicFolderSettingsCommand.java (line 27)`
- `airsonic-main/src/main/java/org/airsonic/player/command/MusicFolderSettingsCommand.java (line 28)`
- `airsonic-main/src/main/java/org/airsonic/player/command/MusicFolderSettingsCommand.java (line 29)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/AvatarUploadController.java (line 49)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/AvatarUploadController.java (line 48)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/AvatarUploadController.java (line 47)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/CaptionsController.java (line 45)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/CaptionsController.java (line 46)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/CaptionsController.java (line 44)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/CoverArtController.java (line 52)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/CoverArtController.java (line 51)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/DatabaseSettingsController.java (line 62)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/DownloadController.java (line 55)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/DownloadController.java (line 54)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/ExternalPlayerController.java (line 42)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/HLSController.java (line 71)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/HelpController.java (line 39)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/HelpController.java (line 40)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/LeftController.java (line 38)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/MoreController.java (line 40)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/UploadController.java (line 68)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/UploadController.java (line 66)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/UploadController.java (line 67)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/UploadController.java (line 69)`
- `airsonic-main/src/main/java/org/airsonic/player/dao/AbstractDao.java (line 39)`
- `airsonic-main/src/main/java/org/airsonic/player/dao/DatabaseDao.java (line 8)`
- `airsonic-main/src/main/java/org/airsonic/player/dao/MusicFolderDao.java (line 35)`
- `airsonic-main/src/main/java/org/airsonic/player/dao/MusicFolderDao.java (line 34)`
- `airsonic-main/src/main/java/org/airsonic/player/domain/Avatar.java (line 22)`
- `airsonic-main/src/main/java/org/airsonic/player/domain/Avatar.java (line 23)`
- `airsonic-main/src/main/java/org/airsonic/player/domain/CoverArt.java (line 3)`
- `airsonic-main/src/main/java/org/airsonic/player/domain/CoverArt.java (line 4)`
- `airsonic-main/src/main/java/org/airsonic/player/domain/MediaFile.java (line 24)`
- `airsonic-main/src/main/java/org/airsonic/player/domain/MediaFile.java (line 25)`
- `airsonic-main/src/main/java/org/airsonic/player/domain/MusicFolder.java (line 25)`
- `airsonic-main/src/main/java/org/airsonic/player/domain/PlayQueue.java (line 24)`
- `airsonic-main/src/main/java/org/airsonic/player/domain/TransferStatus.java (line 24)`
- `airsonic-main/src/main/java/org/airsonic/player/filter/BootstrapVerificationFilter.java (line 30)`
- `airsonic-main/src/main/java/org/airsonic/player/filter/BootstrapVerificationFilter.java (line 31)`
- `airsonic-main/src/main/java/org/airsonic/player/io/TranscodeInputStream.java (line 30)`
- `airsonic-main/src/main/java/org/airsonic/player/io/TranscodeInputStream.java (line 31)`
- `airsonic-main/src/main/java/org/airsonic/player/service/ConfigurationPropertiesService.java (line 14)`
- `airsonic-main/src/main/java/org/airsonic/player/service/ConfigurationPropertiesService.java (line 13)`
- `airsonic-main/src/main/java/org/airsonic/player/service/CoverArtService.java (line 16)`
- `airsonic-main/src/main/java/org/airsonic/player/service/DatabaseService.java (line 31)`
- `airsonic-main/src/main/java/org/airsonic/player/service/DatabaseService.java (line 33)`
- `airsonic-main/src/main/java/org/airsonic/player/service/DatabaseService.java (line 32)`
- `airsonic-main/src/main/java/org/airsonic/player/service/DatabaseService.java (line 30)`
- `airsonic-main/src/main/java/org/airsonic/player/service/LastFmCache.java (line 29)`
- `airsonic-main/src/main/java/org/airsonic/player/service/LastFmCache.java (line 31)`
- `airsonic-main/src/main/java/org/airsonic/player/service/LastFmCache.java (line 30)`
- `airsonic-main/src/main/java/org/airsonic/player/service/LastFmService.java (line 36)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MediaFileService.java (line 47)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MediaFileService.java (line 48)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MediaFileService.java (line 49)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MediaFolderService.java (line 16)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MediaFolderService.java (line 15)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MediaScannerService.java (line 40)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MusicIndexService.java (line 29)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PathWatcherService.java (line 15)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PathWatcherService.java (line 20)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PathWatcherService.java (line 14)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PathWatcherService.java (line 16)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PathWatcherService.java (line 17)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PathWatcherService.java (line 19)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PathWatcherService.java (line 18)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PlaylistService.java (line 53)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PlaylistService.java (line 52)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PlaylistService.java (line 50)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PlaylistService.java (line 51)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PodcastService.java (line 71)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PodcastService.java (line 70)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PodcastService.java (line 69)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SecurityService.java (line 54)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SecurityService.java (line 55)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SecurityService.java (line 56)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SecurityService.java (line 52)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SecurityService.java (line 53)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 49)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 51)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 50)`
- `airsonic-main/src/main/java/org/airsonic/player/service/TranscodingService.java (line 42)`
- `airsonic-main/src/main/java/org/airsonic/player/service/TranscodingService.java (line 40)`
- `airsonic-main/src/main/java/org/airsonic/player/service/TranscodingService.java (line 41)`
- `airsonic-main/src/main/java/org/airsonic/player/service/hls/HlsSession.java (line 18)`
- `airsonic-main/src/main/java/org/airsonic/player/service/hls/HlsSession.java (line 17)`
- `airsonic-main/src/main/java/org/airsonic/player/service/metadata/DefaultMetaDataParser.java (line 29)`
- `airsonic-main/src/main/java/org/airsonic/player/service/metadata/DefaultMetaDataParser.java (line 28)`
- `airsonic-main/src/main/java/org/airsonic/player/service/metadata/FFmpegParser.java (line 40)`
- `airsonic-main/src/main/java/org/airsonic/player/service/metadata/FFmpegParser.java (line 39)`
- `airsonic-main/src/main/java/org/airsonic/player/service/metadata/JaudiotaggerParser.java (line 41)`
- `airsonic-main/src/main/java/org/airsonic/player/service/metadata/JaudiotaggerParser.java (line 40)`
- `airsonic-main/src/main/java/org/airsonic/player/service/metadata/MetaDataParser.java (line 28)`
- `airsonic-main/src/main/java/org/airsonic/player/service/metadata/MetaDataParserFactory.java (line 25)`
- `airsonic-main/src/main/java/org/airsonic/player/service/playlist/DefaultPlaylistImportHandler.java (line 9)`
- `airsonic-main/src/main/java/org/airsonic/player/service/playlist/PlaylistImportHandler.java (line 13)`
- `airsonic-main/src/main/java/org/airsonic/player/service/playlist/PlaylistImportHandler.java (line 14)`
- `airsonic-main/src/main/java/org/airsonic/player/service/playlist/XspfPlaylistImportHandler.java (line 10)`
- `airsonic-main/src/main/java/org/airsonic/player/service/search/IndexManager.java (line 41)`
- `airsonic-main/src/main/java/org/airsonic/player/service/search/IndexManager.java (line 42)`
- `airsonic-main/src/main/java/org/airsonic/player/spring/migrations/AddMediaFileIdToPodcastChannels.java (line 16)`
- `airsonic-main/src/main/java/org/airsonic/player/spring/migrations/AddMediaFileIdToPodcastChannels.java (line 17)`
- `airsonic-main/src/main/java/org/airsonic/player/spring/migrations/AddUnknownMediaFolders.java (line 14)`
- `airsonic-main/src/main/java/org/airsonic/player/spring/migrations/ExtractCustomAvatars.java (line 19)`
- `airsonic-main/src/main/java/org/airsonic/player/spring/migrations/ExtractCustomAvatars.java (line 18)`
- `airsonic-main/src/main/java/org/airsonic/player/util/FileUtil.java (line 30)`
- `airsonic-main/src/main/java/org/airsonic/player/util/FileUtil.java (line 29)`
- `airsonic-main/src/main/java/org/airsonic/player/util/FileUtil.java (line 28)`
- `airsonic-main/src/main/java/org/airsonic/player/util/LegacyHsqlMigrationUtil.java (line 15)`
- `airsonic-main/src/main/java/org/airsonic/player/util/LegacyHsqlMigrationUtil.java (line 14)`
- `airsonic-main/src/main/java/org/airsonic/player/command/MusicFolderSettingsCommand.java (line 260)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/CaptionsController.java (line 111)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/HelpController.java (line 93)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/UploadController.java (line 152)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/UploadController.java (line 178)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/UploadController.java (line 400)`
- `airsonic-main/src/main/java/org/airsonic/player/dao/MusicFolderDao.java (line 198)`
- `airsonic-main/src/main/java/org/airsonic/player/domain/Avatar.java (line 42)`
- `airsonic-main/src/main/java/org/airsonic/player/domain/CoverArt.java (line 63)`
- `airsonic-main/src/main/java/org/airsonic/player/domain/MediaFile.java (line 139)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MediaFileService.java (line 87)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MediaFileService.java (line 115)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MediaFileService.java (line 123)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MediaScannerService.java (line 219)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MusicIndexService.java (line 75)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MusicIndexService.java (line 82)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MusicIndexService.java (line 163)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PlaylistService.java (line 122)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PlaylistService.java (line 314)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SecurityService.java (line 433)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SecurityService.java (line 434)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 70)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 71)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 399)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 401)`
- `airsonic-main/src/main/java/org/airsonic/player/service/playlist/PlaylistImportHandler.java (line 37)`
- `airsonic-main/src/main/java/org/airsonic/player/service/playlist/PlaylistImportHandler.java (line 64)`
- `airsonic-main/src/main/java/org/airsonic/player/spring/migrations/AddMediaFileIdToPodcastChannels.java (line 60)`
- `airsonic-main/src/main/java/org/airsonic/player/spring/migrations/AddUnknownMediaFolders.java (line 84)`
- `airsonic-main/src/main/java/org/airsonic/player/util/LegacyHsqlMigrationUtil.java (line 31)`
- `airsonic-main/src/main/java/org/airsonic/player/util/LegacyHsqlMigrationUtil.java (line 115)`

</details>

<details id="CRA_Default_or_well-known_password_detected">
<summary><b>CRA: Default or well-known password detected</b> — affected files</summary>

- `airsonic-main/src/test/java/org/airsonic/player/api/AirsonicRestApiIntTest.java (line 27)`
- `airsonic-main/src/test/java/org/airsonic/player/api/CORSTest.java (line 31)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 24)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 294)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 322)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 329)`
- `airsonic-main/src/main/java/org/airsonic/player/command/DatabaseSettingsCommand.java (line 42)`
- `airsonic-main/src/main/java/org/airsonic/player/service/scrobbler/LastFMScrobbler.java (line 102)`
- `airsonic-main/src/test/java/org/airsonic/player/security/RESTAuthTest.java (line 28)`
- `airsonic-main/src/main/java/org/airsonic/player/command/UserSettingsCommand.java (line 207)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 24)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 354)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 397)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 404)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 643)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 651)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 322)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 24)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 294)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 322)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 509)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 517)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 24)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 294)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 322)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 367)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 509)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 24)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 294)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 322)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 509)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 517)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 517)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 329)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 329)`

</details>

<details id="Hardcoded_IP_Address">
<summary><b>Hardcoded IP Address</b> — affected files</summary>

- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 575)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 466)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 575)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 474)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 474)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 473)`

</details>

<details id="Caching_-_Spring_Boot_Cache_library">
<summary><b>Caching - Spring Boot Cache library</b> — affected files</summary>

- `airsonic-main/pom.xml (line 79)`
- `airsonic-main/src/main/java/org/airsonic/player/service/CoverArtService.java (line 11)`
- `airsonic-main/src/main/java/org/airsonic/player/service/CoverArtService.java (line 12)`
- `airsonic-main/src/main/java/org/airsonic/player/service/CoverArtService.java (line 13)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MediaFileService.java (line 43)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MediaFileService.java (line 42)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MediaFileService.java (line 41)`
- `airsonic-main/src/main/java/org/airsonic/player/service/MediaFolderService.java (line 12)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PlaylistService.java (line 40)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PlaylistService.java (line 39)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SecurityService.java (line 37)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SecurityService.java (line 39)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SecurityService.java (line 38)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 39)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 38)`
- `airsonic-main/src/main/java/org/airsonic/player/spring/CacheConfiguration.java (line 22)`

</details>

<details id="File_system_-_Java_IO">
<summary><b>File system - Java IO</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/controller/HLSController.java (line 70)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/M3UController.java (line 39)`
- `airsonic-main/src/main/java/org/airsonic/player/dao/MusicFolderDao.java (line 33)`
- `airsonic-main/src/main/java/org/airsonic/player/io/InputStreamReaderThread.java (line 26)`
- `airsonic-main/src/main/java/org/airsonic/player/io/PipeStreams.java (line 7)`
- `airsonic-main/src/main/java/org/airsonic/player/service/jukebox/PlayerTest.java (line 6)`
- `airsonic-main/src/main/java/org/airsonic/player/spring/WebsocketConfiguration.java (line 33)`
- `airsonic-main/src/main/java/org/airsonic/player/upload/MonitoredDiskFileItem.java (line 24)`
- `airsonic-main/src/main/java/org/airsonic/player/upload/MonitoredDiskFileItemFactory.java (line 25)`
- `airsonic-main/src/main/java/org/airsonic/player/util/FileUtil.java (line 26)`

</details>

<details id="Local_HTTP_Calls">
<summary><b>Local HTTP Calls</b> — affected files</summary>

- `airsonic-main/src/test/java/org/airsonic/player/service/JWTSecurityServiceTest.java (line 31)`
- `integration-test/src/test/java/org/airsonic/test/Scanner.java (line 35)`
- `integration-test/pom.xml (line 16)`
- `airsonic-main/src/test/resources/airsonic-rest-api-soapui-project.xml (line 2)`
- `airsonic-main/src/test/resources/airsonic-rest-api-soapui-project.xml (line 8)`
- `airsonic-main/src/test/resources/airsonic-rest-api-soapui-project.xml (line 15)`
- `airsonic-main/src/test/resources/airsonic-rest-api-soapui-project.xml (line 18)`
- `airsonic-main/src/test/resources/airsonic-rest-api-soapui-project.xml (line 21)`
- `airsonic-main/src/test/resources/airsonic-rest-api-soapui-project.xml (line 25)`
- `airsonic-main/src/test/resources/airsonic-rest-api-soapui-project.xml (line 30)`

</details>

<details id="File_system_-_java_net_URL_URI">
<summary><b>File system - java.net.URL/URI</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/InternetRadioService.java (line 164)`
- `airsonic-main/src/main/java/org/airsonic/player/service/InternetRadioService.java (line 235)`
- `airsonic-main/src/main/java/org/airsonic/player/service/NetworkService.java (line 103)`
- `airsonic-main/src/main/java/org/airsonic/player/service/NetworkService.java (line 114)`
- `airsonic-main/src/main/java/org/airsonic/player/service/NetworkService.java (line 122)`
- `airsonic-main/src/main/java/org/airsonic/player/service/NetworkService.java (line 127)`
- `airsonic-main/src/main/java/org/airsonic/player/service/scrobbler/LastFMScrobbler.java (line 163)`
- `airsonic-main/src/main/java/org/airsonic/player/util/StringUtil.java (line 331)`

</details>

<details id="CRA_Hard-coded_password_in_Java_source_code">
<summary><b>CRA: Hard-coded password in Java source code</b> — affected files</summary>

- `airsonic-main/src/test/java/org/airsonic/player/api/AirsonicRestApiIntTest.java (line 27)`
- `airsonic-main/src/test/java/org/airsonic/player/security/RESTAuthTest.java (line 28)`
- `airsonic-main/src/main/java/org/airsonic/player/security/AesGcmPasswordEncoder.java (line 40)`
- `airsonic-main/src/test/java/org/airsonic/player/api/CORSTest.java (line 31)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 118)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 137)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 147)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 161)`

</details>

<details id="Java_Native_Processes">
<summary><b>Java Native Processes</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/controller/CaptionsController.java (line 159)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 427)`
- `airsonic-main/src/main/java/org/airsonic/player/service/metadata/FFmpegParser.java (line 88)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 429)`
- `airsonic-main/src/main/java/org/airsonic/player/service/TranscodingService.java (line 386)`
- `airsonic-main/src/main/java/org/airsonic/player/io/TranscodeInputStream.java (line 59)`
- `airsonic-main/src/main/java/org/airsonic/player/service/TranscodingService.java (line 369)`

</details>

<details id="File_system_-_file_scheme">
<summary><b>File system - 'file://' scheme</b> — affected files</summary>

- `airsonic-main/src/test/java/org/airsonic/player/service/PlaylistServiceTestImport.java (line 164)`
- `airsonic-main/src/test/java/org/airsonic/player/service/PlaylistServiceTestImport.java (line 165)`
- `airsonic-main/src/test/java/org/airsonic/player/service/PlaylistServiceTestImport.java (line 166)`
- `pom.xml (line 36)`
- `pom.xml (line 51)`

</details>

<details id="CRA_Use_of_weak_hash_algorithm_MD5">
<summary><b>CRA: Use of weak hash algorithm MD5</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/scrobbler/LastFMScrobbler.java (line 229)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SecurityService.java (line 452)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/CoverArtController.java (line 235)`
- `integration-test/src/test/java/org/airsonic/test/Scanner.java (line 55)`

</details>

<details id="Apache_Commons_I_O">
<summary><b>Apache Commons I/O</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/controller/CaptionsController.java (line 18)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/HelpController.java (line 25)`
- `airsonic-main/src/main/java/org/airsonic/player/service/InternetRadioService.java (line 6)`

</details>

<details id="CRA_Insecure_TLS_cipher_suites">
<summary><b>CRA: Insecure TLS cipher suites</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 149)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 237)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 1664)`

</details>

<details id="CRA_Use_of_insecure_random_number_generator_java_util_Random">
<summary><b>CRA: Use of insecure random number generator java.util.Random</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/search/SearchServiceImpl.java (line 53)`
- `airsonic-main/src/test/java/org/airsonic/player/service/search/SearchServiceTestCase.java (line 393)`

</details>

<details id="CRA_Use_of_weak_password_hashing_plain_MD5_SHA_for_passwords">
<summary><b>CRA: Use of weak password hashing (plain MD5/SHA for passwords)</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/security/GlobalSecurityConfig.java (line 77)`
- `airsonic-main/src/main/java/org/airsonic/player/security/GlobalSecurityConfig.java (line 87)`

</details>

<details id="CRA_Hard-coded_API_key_or_secret_in_Java_source_code">
<summary><b>CRA: Hard-coded API key or secret in Java source code</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 155)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 253)`

</details>

<details id="CRA_Use_of_weak_hash_algorithm_SHA-1">
<summary><b>CRA: Use of weak hash algorithm SHA-1</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/podcast/PodcastIndexService.java (line 76)`

</details>

<details id="Avoid_File_System_Logging_in_Configuration">
<summary><b>Avoid File System Logging in Configuration</b> — affected files</summary>

- `airsonic-main/src/test/resources/logback.xml (line 5)`

</details>

<details id="Caching_-_Ehcache_embedded_library">
<summary><b>Caching - Ehcache embedded library</b> — affected files</summary>

- `airsonic-main/pom.xml (line 87)`

</details>

<details id="Java_Mail_API">
<summary><b>Java Mail API</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/controller/RecoverController.java (line 21)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/RecoverController.java (line 22)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/RecoverController.java (line 18)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/RecoverController.java (line 20)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/RecoverController.java (line 19)`
- `airsonic-main/pom.xml (line 294)`

</details>

<details id="The_java_net_URLEncoder_encode_method_uses_UTF-8_by_default">
<summary><b>The java.net.URLEncoder.encode method uses UTF-8 by default</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/controller/CaptionsController.java (line 231)`
- `airsonic-main/src/main/java/org/airsonic/player/taglib/UrlTag.java (line 134)`
- `airsonic-main/src/main/java/org/airsonic/player/util/StringUtil.java (line 271)`

</details>

<details id="Embedded_framework_-_Spring_Security">
<summary><b>Embedded framework - Spring Security</b> — affected files</summary>

- `airsonic-main/pom.xml (line 115)`
- `airsonic-main/pom.xml (line 71)`

</details>

<details id="MySQL_database_found">
<summary><b>MySQL database found</b> — affected files</summary>

- `airsonic-main/pom.xml (line 555)`

</details>

<details id="PostgreSQL_database_found">
<summary><b>PostgreSQL database found</b> — affected files</summary>

- `airsonic-main/pom.xml (line 544)`

</details>

<details id="The_java_io_constructor_defaults_to_UTF-8">
<summary><b>The 'java.io' constructor defaults to UTF-8</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/io/InputStreamReaderThread.java (line 53)`
- `airsonic-main/src/main/java/org/airsonic/player/util/StringUtil.java (line 233)`

</details>

<details id="The_java_net_URLDecoder_decode_method_uses_UTF-8_by_default">
<summary><b>The java.net.URLDecoder.decode method uses UTF-8 by default</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/PodcastService.java (line 251)`
- `airsonic-main/src/main/java/org/airsonic/player/util/StringUtil.java (line 282)`

</details>

<details id="Password_found_in_configuration_file">
<summary><b>Password found in configuration file</b> — affected files</summary>

- `airsonic-main/src/test/resources/application.properties (line 14)`

</details>

<details id="Java_Swing_usage">
<summary><b>Java Swing usage</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/jukebox/PlayerTest.java (line 3)`

</details>

<details id="Avoid_using_hardcoded_URLs_HTTP_protocol_in_source_code">
<summary><b>Avoid using hardcoded URLs (HTTP protocol) in source code</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/VersionService.java (line 208)`
- `airsonic-main/src/main/java/org/airsonic/player/service/sonos/SonosServiceRegistration.java (line 95)`
- `airsonic-main/src/main/java/org/airsonic/player/service/sonos/SonosLinkSecurityInterceptor.java (line 142)`
- `airsonic-main/src/main/java/org/airsonic/player/service/podcast/PodcastIndexService.java (line 39)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_is.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_no.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 366)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 369)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 376)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 474)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 524)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 528)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 641)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 702)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_es.properties (line 726)`
- `airsonic-main/src/main/java/org/airsonic/player/util/XMLUtil.java (line 9)`
- `airsonic-main/src/main/java/org/airsonic/player/util/XMLUtil.java (line 10)`
- `airsonic-main/src/main/java/org/airsonic/player/util/XMLUtil.java (line 11)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 475)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ja_JP.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nn.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en_GB.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_et.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 207)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 213)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 279)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 307)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 309)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 330)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 452)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 453)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 455)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 464)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 575)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 683)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 687)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 822)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 829)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 907)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 909)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_da.properties (line 933)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 145)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 146)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 152)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 216)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 242)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 244)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 263)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 358)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 361)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 466)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 515)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 519)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 623)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 680)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 682)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_BR.properties (line 706)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt_PT.properties (line 724)`
- `airsonic-main/src/main/java/org/airsonic/player/ajax/LyricsWSController.java (line 78)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pt.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ko.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_CN.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_nl.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_it.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_pl.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fi.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_bg.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_mk.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_el.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 475)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_de.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ru.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 150)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 156)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 222)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 248)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 250)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 271)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 366)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 369)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 376)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 474)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 530)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 534)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 647)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 705)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 707)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_fr.properties (line 731)`
- `airsonic-main/src/main/java/org/airsonic/player/service/scrobbler/ListenBrainzScrobbler.java (line 92)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_cs.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 206)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 207)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 213)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 279)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 307)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 309)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 330)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 452)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 453)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 455)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 464)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 575)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 683)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 687)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 821)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 828)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 906)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 908)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_en.properties (line 932)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_ca.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sv.properties (line 724)`
- `airsonic-main/src/test/java/org/airsonic/player/service/PlaylistServiceTestImport.java (line 156)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_sl.properties (line 724)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 25)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 26)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 27)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 28)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 29)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 30)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 31)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 32)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 33)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 34)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 35)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 37)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 38)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 43)`
- `airsonic-main/src/test/java/org/airsonic/player/service/InternetRadioServiceTest.java (line 45)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 475)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_zh_TW.properties (line 724)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 148)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 149)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 155)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 221)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 247)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 249)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 270)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 365)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 368)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 375)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 473)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 523)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 527)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 640)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 698)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 700)`
- `airsonic-main/src/main/resources/org/airsonic/player/i18n/ResourceBundle_uk.properties (line 724)`
- `integration-test/src/test/java/org/airsonic/test/Scanner.java (line 35)`
- `airsonic-main/src/test/java/org/airsonic/player/service/JWTSecurityServiceTest.java (line 31)`
- `airsonic-main/src/test/java/org/airsonic/player/api/CORSTest.java (line 71)`
- `airsonic-main/src/test/java/org/airsonic/player/api/CORSTest.java (line 86)`
- `airsonic-main/src/test/java/org/airsonic/player/api/CORSTest.java (line 95)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PodcastService.java (line 101)`
- `airsonic-main/src/main/java/org/airsonic/player/service/PodcastService.java (line 102)`
- `airsonic-main/src/test/java/org/airsonic/player/util/StringUtilTestCase.java (line 141)`
- `airsonic-main/src/test/java/org/airsonic/player/util/StringUtilTestCase.java (line 142)`
- `airsonic-main/src/test/java/org/airsonic/player/util/StringUtilTestCase.java (line 143)`
- `airsonic-main/src/test/java/org/airsonic/player/util/StringUtilTestCase.java (line 144)`
- `airsonic-main/src/test/java/org/airsonic/player/util/StringUtilTestCase.java (line 146)`
- `airsonic-main/src/test/java/org/airsonic/player/util/StringUtilTestCase.java (line 147)`
- `airsonic-main/src/test/java/org/airsonic/player/util/StringUtilTestCase.java (line 148)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 33)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 43)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 45)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 46)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 47)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 50)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 53)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 58)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 62)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 65)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 80)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 97)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 111)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 148)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 153)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 175)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 195)`
- `airsonic-main/src/test/java/org/airsonic/player/dao/PodcastDaoTestCase.java (line 206)`

</details>

<details id="Java_LDAP_usage_detected">
<summary><b>Java LDAP usage detected</b> — affected files</summary>

- `airsonic-main/pom.xml (line 120)`
- `airsonic-main/pom.xml (line 129)`
- `airsonic-main/src/main/java/org/airsonic/player/security/CustomLDAPAuthenticatorPostProcessor.java (line 18)`
- `airsonic-main/src/main/java/org/airsonic/player/security/CustomLDAPAuthenticatorPostProcessor.java (line 17)`
- `airsonic-main/src/main/java/org/airsonic/player/security/CustomUserDetailsContextMapper.java (line 27)`
- `airsonic-main/src/main/java/org/airsonic/player/security/CustomUserDetailsContextMapper.java (line 26)`
- `airsonic-main/src/main/java/org/airsonic/player/security/CustomUserDetailsContextMapper.java (line 25)`
- `airsonic-main/src/main/java/org/airsonic/player/security/CustomUserDetailsContextMapper.java (line 28)`
- `airsonic-main/src/main/java/org/airsonic/player/security/CustomLDAPAuthenticatorPostProcessor.java (line 11)`
- `airsonic-main/src/main/java/org/airsonic/player/security/CustomUserDetailsContextMapper.java (line 22)`
- `airsonic-main/src/main/java/org/airsonic/player/security/CustomUserDetailsContextMapper.java (line 21)`

</details>

<details id="Environment_variables_system_properties">
<summary><b>Environment variables/system properties</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/TomcatApplication.java (line 20)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/GettingStartedController.java (line 52)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/HelpController.java (line 82)`
- `airsonic-main/src/main/java/org/airsonic/player/controller/HelpController.java (line 83)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 396)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 397)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 503)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 1116)`
- `airsonic-main/src/main/java/org/airsonic/player/util/Util.java (line 63)`
- `airsonic-main/src/main/java/org/airsonic/player/util/Util.java (line 68)`
- `airsonic-main/src/main/java/org/airsonic/player/util/Util.java (line 73)`

</details>

<details id="Avoid_using_hardcoded_URLs_LDAP_protocol_in_source_code">
<summary><b>Avoid using hardcoded URLs (LDAP protocol) in source code</b> — affected files</summary>

- `airsonic-main/src/test/java/org/airsonic/player/service/SettingsServiceTestCase.java (line 95)`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java (line 219)`

</details>

<details id="Socket_communication">
<summary><b>Socket communication</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/ajax/LyricsWSController.java (line 44)`

</details>

## Upgrade Issues

| Issue Name | Criticality | Story Points | Occurrences |
|------------|-------------|--------------|-------------|
| Spring Boot Version Has Reached the End of OSS Support | Mandatory | 8 | [10](#Spring_Boot_Version_Has_Reached_the_End_of_OSS_Support) |
| Spring Framework Version Has Reached the End of OSS Support | Mandatory | 8 | [10](#Spring_Framework_Version_Has_Reached_the_End_of_OSS_Support) |
| Jakarta EE Version is not the latest stable | Optional | 8 | [3](#Jakarta_EE_Version_is_not_the_latest_stable) |
| Java Version is not the latest LTS | Optional | 8 | [1](#Java_Version_is_not_the_latest_LTS) |

### Issue Details

<details id="Spring_Boot_Version_Has_Reached_the_End_of_OSS_Support">
<summary><b>Spring Boot Version Has Reached the End of OSS Support</b> — affected files</summary>

- `airsonic-main/pom.xml (line 63)`
- `airsonic-main/pom.xml (line 53)`
- `airsonic-main/pom.xml (line 99)`
- `airsonic-main/pom.xml (line 95)`
- `airsonic-main/pom.xml (line 71)`
- `airsonic-main/pom.xml (line 79)`
- `airsonic-main/pom.xml (line 75)`
- `airsonic-main/pom.xml (line 519)`
- `airsonic-main/pom.xml (line 67)`
- `airsonic-main/pom.xml (line 509)`

</details>

<details id="Spring_Framework_Version_Has_Reached_the_End_of_OSS_Support">
<summary><b>Spring Framework Version Has Reached the End of OSS Support</b> — affected files</summary>

- `airsonic-main/pom.xml (line 99)`
- `airsonic-main/pom.xml (line 75)`
- `airsonic-main/pom.xml (line 120)`
- `airsonic-main/pom.xml (line 67)`
- `airsonic-main/pom.xml (line 133)`
- `airsonic-main/pom.xml (line 53)`
- `airsonic-main/pom.xml (line 71)`
- `airsonic-main/pom.xml (line 311)`
- `airsonic-main/pom.xml (line 79)`
- `airsonic-main/pom.xml (line 91)`

</details>

<details id="Jakarta_EE_Version_is_not_the_latest_stable">
<summary><b>Jakarta EE Version is not the latest stable</b> — affected files</summary>

- `airsonic-main/pom.xml (line 441)`
- `airsonic-main/pom.xml (line 430)`
- `airsonic-main/pom.xml (line 83)`

</details>

<details id="Java_Version_is_not_the_latest_LTS">
<summary><b>Java Version is not the latest LTS</b> — affected files</summary>

- `pom.xml (line 27)`

</details>

## Security Issues

> **Note:** These issues were generated by AI and may contain inaccuracies or incomplete information. Please review carefully.

| Issue Name | Criticality | Story Points | Files |
|------------|-------------|--------------|-------|
| CWE-77 | Mandatory | 13 | [1](#CWE-77) |
| CWE-78 | Mandatory | 13 | [3](#CWE-78) |
| CWE-89 | Mandatory | 13 | [1](#CWE-89) |
| CWE-434 | Mandatory | 8 | [1](#CWE-434) |
| CWE-662 | Potential | 8 | [1](#CWE-662) |
| CWE-667 | Potential | 8 | [1](#CWE-667) |
| CWE-820 | Potential | 8 | [1](#CWE-820) |
| CWE-821 | Potential | 8 | [1](#CWE-821) |
| CWE-682 | Potential | 5 | [1](#CWE-682) |
| CWE-772 | Potential | 5 | [1](#CWE-772) |
| CWE-775 | Potential | 5 | [1](#CWE-775) |
| CWE-789 | Potential | 5 | [1](#CWE-789) |
| CWE-835 | Potential | 5 | [1](#CWE-835) |
| CWE-543 | Potential | 5 | [1](#CWE-543) |
| CWE-567 | Potential | 5 | [1](#CWE-567) |
| CWE-99 | Potential | 3 | [1](#CWE-99) |
| CWE-130 | Potential | 3 | [1](#CWE-130) |
| CWE-606 | Potential | 3 | [1](#CWE-606) |
| CWE-665 | Potential | 3 | [1](#CWE-665) |
| CWE-681 | Potential | 3 | [1](#CWE-681) |
| CWE-783 | Potential | 3 | [1](#CWE-783) |
| CWE-778 | Potential | 3 | [3](#CWE-778) |
| CWE-456 | Potential | 2 | [1](#CWE-456) |
| CWE-457 | Potential | 2 | [1](#CWE-457) |
| CWE-22 | Optional | 8 | [1](#CWE-22) |
| CWE-79 | Optional | 8 | [1](#CWE-79) |
| CWE-23 | Optional | 5 | [1](#CWE-23) |
| CWE-88 | Optional | 5 | [1](#CWE-88) |
| CWE-90 | Optional | 5 | [2](#CWE-90) |
| CWE-259 | Optional | 5 | [2](#CWE-259) |
| CWE-732 | Optional | 5 | [2](#CWE-732) |
| CWE-798 | Optional | 5 | [2](#CWE-798) |
| CWE-1057 | Optional | 3 | [1](#CWE-1057) |
| CWE-477 | Optional | 1 | [1](#CWE-477) |
| CVE-2022-41853 | Optional | 1 | [1](#CVE-2022-41853) |
| CVE-2023-24998 | Optional | 1 | [1](#CVE-2023-24998) |
| CVE-2025-48976 | Optional | 1 | [1](#CVE-2025-48976) |
| CVE-2025-23184 | Optional | 1 | [1](#CVE-2025-23184) |
| CVE-2025-48734 | Optional | 1 | [1](#CVE-2025-48734) |
| CVE-2021-33813 | Optional | 1 | [1](#CVE-2021-33813) |
| CVE-2026-42198 | Optional | 1 | [1](#CVE-2026-42198) |
| CVE-2025-49146 | Optional | 1 | [1](#CVE-2025-49146) |

### Security Issue Details

<details id="CWE-77">
<summary><b>CWE-77</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/controller/TranscodingSettingsController.java`

</details>

<details id="CWE-78">
<summary><b>CWE-78</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/TranscodingService.java`
- `airsonic-main/src/main/java/org/airsonic/player/service/metadata/FFmpegParser.java`
- `airsonic-main/src/main/java/org/airsonic/player/controller/CaptionsController.java`

</details>

<details id="CWE-89">
<summary><b>CWE-89</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/dao/MediaFileDao.java`

</details>

<details id="CWE-434">
<summary><b>CWE-434</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/controller/UploadController.java`

</details>

<details id="CWE-662">
<summary><b>CWE-662</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/domain/PlayQueue.java`

</details>

<details id="CWE-667">
<summary><b>CWE-667</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/JukeboxLegacySubsonicService.java`

</details>

<details id="CWE-820">
<summary><b>CWE-820</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/InternetRadioService.java`

</details>

<details id="CWE-821">
<summary><b>CWE-821</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/MediaScannerService.java`

</details>

<details id="CWE-682">
<summary><b>CWE-682</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/controller/SubsonicRESTController.java`

</details>

<details id="CWE-772">
<summary><b>CWE-772</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/controller/CaptionsController.java`

</details>

<details id="CWE-775">
<summary><b>CWE-775</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/hls/HlsSession.java`

</details>

<details id="CWE-789">
<summary><b>CWE-789</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/controller/SubsonicRESTController.java`

</details>

<details id="CWE-835">
<summary><b>CWE-835</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/scrobbler/LastFMScrobbler.java`

</details>

<details id="CWE-543">
<summary><b>CWE-543</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/InternetRadioService.java`

</details>

<details id="CWE-567">
<summary><b>CWE-567</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/domain/PlayQueue.java`

</details>

<details id="CWE-99">
<summary><b>CWE-99</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/MediaFileService.java`

</details>

<details id="CWE-130">
<summary><b>CWE-130</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/controller/StreamController.java`

</details>

<details id="CWE-606">
<summary><b>CWE-606</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/controller/SubsonicRESTController.java`

</details>

<details id="CWE-665">
<summary><b>CWE-665</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/scrobbler/LastFMScrobbler.java`

</details>

<details id="CWE-681">
<summary><b>CWE-681</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/sonos/SonosHelper.java`

</details>

<details id="CWE-783">
<summary><b>CWE-783</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/domain/PlayQueue.java`

</details>

<details id="CWE-778">
<summary><b>CWE-778</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/security/JWTRequestParameterProcessingFilter.java`
- `airsonic-main/src/main/java/org/airsonic/player/security/MultipleCredsMatchingAuthenticationProvider.java`
- `airsonic-main/src/main/java/org/airsonic/player/security/GlobalSecurityConfig.java`

</details>

<details id="CWE-456">
<summary><b>CWE-456</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/DatabaseService.java`

</details>

<details id="CWE-457">
<summary><b>CWE-457</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/DatabaseService.java`

</details>

<details id="CWE-22">
<summary><b>CWE-22</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/controller/AvatarUploadController.java`

</details>

<details id="CWE-79">
<summary><b>CWE-79</b> — affected files</summary>

- `airsonic-main/src/main/webapp/WEB-INF/jsp/coverArt.jsp`

</details>

<details id="CWE-23">
<summary><b>CWE-23</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/playlist/PlaylistImportHandler.java`

</details>

<details id="CWE-88">
<summary><b>CWE-88</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/TranscodingService.java`

</details>

<details id="CWE-90">
<summary><b>CWE-90</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/security/GlobalSecurityConfig.java`
- `airsonic-main/src/main/java/org/airsonic/player/controller/AdvancedSettingsController.java`

</details>

<details id="CWE-259">
<summary><b>CWE-259</b> — affected files</summary>

- `airsonic-main/src/main/resources/liquibase/legacy/schema25.xml`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java`

</details>

<details id="CWE-732">
<summary><b>CWE-732</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/ConfigurationPropertiesService.java`
- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java`

</details>

<details id="CWE-798">
<summary><b>CWE-798</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/service/SettingsService.java`
- `airsonic-main/src/main/resources/liquibase/legacy/schema25.xml`

</details>

<details id="CWE-1057">
<summary><b>CWE-1057</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/controller/SubsonicRESTController.java`

</details>

<details id="CWE-477">
<summary><b>CWE-477</b> — affected files</summary>

- `airsonic-main/src/main/java/org/airsonic/player/filter/ParameterDecodingFilter.java`

</details>

<details id="CVE-2022-41853">
<summary><b>CVE-2022-41853</b> — affected files</summary>

- `airsonic-main/pom.xml:252`

</details>

<details id="CVE-2023-24998">
<summary><b>CVE-2023-24998</b> — affected files</summary>

- `airsonic-main/pom.xml:182`

</details>

<details id="CVE-2025-48976">
<summary><b>CVE-2025-48976</b> — affected files</summary>

- `airsonic-main/pom.xml:182`

</details>

<details id="CVE-2025-23184">
<summary><b>CVE-2025-23184</b> — affected files</summary>

- `pom.xml:28`

</details>

<details id="CVE-2025-48734">
<summary><b>CVE-2025-48734</b> — affected files</summary>

- `airsonic-main/pom.xml:420`

</details>

<details id="CVE-2021-33813">
<summary><b>CVE-2021-33813</b> — affected files</summary>

- `airsonic-main/pom.xml:266`

</details>

<details id="CVE-2026-42198">
<summary><b>CVE-2026-42198</b> — affected files</summary>

- `airsonic-main/pom.xml:544`

</details>

<details id="CVE-2025-49146">
<summary><b>CVE-2025-49146</b> — affected files</summary>

- `airsonic-main/pom.xml:544`

</details>

---

## Codebase Insights

> **Note:** These documents are generated by AI and may contain inaccuracies or incomplete information. Please review carefully.

1. **[Architecture Diagram](facts/architecture-diagram.md)** — Understand the big picture: system layers and component relationships
2. **[Dependency Map](facts/dependency-map.md)** — Know what the project depends on and where the risks are
3. **[API & Service Contracts](facts/api-service-contracts.md)** — See how services communicate and what contracts they expose
4. **[Data Architecture](facts/data-architecture.md)** — Explore data models, storage, and data flow patterns
5. **[Configuration Inventory](facts/configuration-inventory.md)** — Review how the application is configured across environments
6. **[Business Workflows](facts/business-workflows.md)** — Trace end-to-end business processes and domain logic

[Share feedback](https://aka.ms/ghcp-appmod/feedback)
