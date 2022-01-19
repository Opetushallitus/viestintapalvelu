Viestintapalvelu
================

### Yleinen kuvaus
Viestintäpalvelu tarjoaa
* Sähköpostin ja ryhmäsähköpostin lähetyspalvelun esim. itserekisteröinnin vahvistussähköposti
* Raportointipalvelun lähetetyistä sähköposteista ja ryhmäsähköposteista
* Asiakirjojen muodostuspalvelun esim. jälkiohjauskirje
* Asiakirja- ja sähköpostipohjien hallintapalvelun esim. jälkiohjauskirjeen pohja

Sähköpostin ja ryhmäsähköpostin lähetyspalvelun avulla muut palvelut voivat lähettää tiedotteita eri sidosryhmille esim. oppilaitosten virkailijoille. Raportointipalvelun avulla käyttäjä voi hakea ja katsella lähetettyjen sähköpostien ja ryhmäsähköpostien tilatietoja esim. onko vastaanottaja saannut ko. sähköpostin onnistuneesti. Asiakirjojen muodostuspalvelun avulla muut palvelut voivat välittää halutut tiedot asiakirjalle ja pyytää muodostamaan asiakirjan. Asiakirja- ja sähköpostipohjien hallintapalvelussa säilytetään pohjia, joita asiakirjojen muodostuspalvelu käyttää ja tarjoaa eri versioita esim. asiakirjasta muille palveluille käytettäväksi.

Tulevaisuudessa viestintäpalvelu tarjoaa ajastus- ja ilmoituspalvelun, minkä avulla voidaan hallita ajastettuja muistutuksia esim. salasanan vaihto ja ilmoitusmuistutuksia käyttäjää kiinnostavista asioista esim. hakulomake on vastaanotettu.

![Viestintäpalvelun arkkitehtuuri](doc/img/arkkitehtuuri.png)


### Ajaminen

mvn clean install

### Teknologiat

Alla olevassa taulukossa on kerrottu tärkeimmät palvelussa käytetyt teknologiat

| Nimi          | Kuvaus                                                                                                      | Käyttö palvelussa            | Linkki                                                  |
|---------------|-------------------------------------------------------------------------------------------------------------|------------------------------|---------------------------------------------------------|
| AngularJS     | AngularJS on rakenteellinen sovelluskehys dynaamisten web-sovellusten rakentamiseen                         | Käyttöliittymä               | https://angularjs.org                                   |
| Apache Camel  | Apache Camel on avoin Java-sovelluskehys helpottamaan sovellusten välistä integraatiota                     | Palvelukerros                | https://camel.apache.org                                |
| Apache CXF    | Apache CXF on avoin sovelluskehys helpottamaan REST- ja SOAP-kehittämistä                                   | Palvelukerros                | http://cxf.apache.org                                   |
| Flying Saucer | Flying Saucer ottaa vastaan XML:ää tai XHTML:ää, yhdistää sen tyylisivuun ja muodostaa iTextin avulla PDF:n | Palvelukerros                | https://code.google.com/p/flying-saucer                 |
| Hibernate     | Hibernate sovelluskehyksen avulla voidaan automatisoida olioiden tallennus relaatiotietokantaan             | Tietokantakerros             | http://hibernate.org                                    |
| iText         | iText on avoin kirjasto, jonka avulla voidaan ylläpitää ja muodostaa PDF-tiedostoja                         | Palvelukerros                | http://itextpdf.com                                     |
| Jackson       | Jackson on joukko apuvälineitä JSON-tiedon käsittelyyn Javassa                                              | Palvelukerros                | https://github.com/FasterXML/jackson                    |
| Jersey        | Jersey tarjoaa REST-palveluiden ja clientien kehittämistä helpottavia välineitä                             | Palvelukerros                | https://jersey.java.net/                                |
| JPA           | Sovellusohjelmointirajapinta relaatiotietokannan käsittelyä varten Java-sovelluksissa                       | Tietokantakerros             | http://docs.oracle.com/javaee/6/tutorial/doc/bnbpz.html |
| JUnit         | Java-yksikkötestien kehittämiseen tarkoitettu kirjasto                                                      | Testaus                      | http://junit.org/                                       |
| Mockito       | Java-yksikkötestien kehittämistä helpottava kirjasto                                                        | Testaus                      | https://code.google.com/p/mockito/                      |
| PowerMock     | Java-yksikkötestien kehittämistä helpottava kirjasto                                                        | Testaus                      | https://code.google.com/p/powermock/                    |
| Spring        | Yleinen sovelluskehys Java-sovellusten kehittämiseen                                                        | Palvelu- ja tietokantakerros | http://spring.io/                                       |
| Swagger       | Kirjasto REST-rajapintojen dokumentointiin. Lisäksi käytössä Swagger-UI Javascript-kirjasto                 | Palvelukerros                | https://github.com/wordnik/swagger-core                 |

### Lisätietoja
Täydellinen tekninen dokumentaatio löytyy [tämän](https://confluence.oph.ware.fi/confluence/pages/viewpage.action?pageId=18186424) linkin takaa.

### Ulkoiset integraatiot

Iposti
Cybercom mailcannon - smtp-palvelu
Kansalaisen asiointitili (ei vielä käytössä, 28.1.2015)

### OPH integraatiot

Viestipalvelu käyttää:
* organisaatiopalvelu
* henkilöpalvelu

Viestipalvelua käytetään:
* Osoitepalvelu

# Kehitysympäristön kasaus
* jdk 1.8
* postgresql (`CREATE DATABASE viestinta`)
* mvn clean test - huom: käyttää globaalisti asennettua boweria viestintapalvelu-ui buildaamiseen

* IDEA/ECLIPSE: konfiguroi tomcat vm-parametrit

```
-Duser.home="/Users/jkytomak/workspace_oph/viestintapalvelu/src/test/resources"
-XX:MaxPermSize=512m
-Dlog4j.configuration="file:///Users/jkytomak/workspace_oph/viestintapalvelu/src/test/resources/oph-configuration/log4j.properties"
```
* IDEA/ECLIPSE: lisää 3 artifaktia tomcatin käynnistykseen ja aseta context polut: /ryhmasahkoposti-service /viestintapalvelu /viestintapalvelu-ui

# Testiosoitteita:

* http://localhost:8080/viestintapalvelu/
** viestintapalveluservice paketin testi ja ylläpitokäyttöliittymä
** viestipohja-jsonien tuonti (Mallien tuonti) ja hakulinkitys

* http://localhost:8080/viestintapalvelu-ui/initpage.jsp
** sähköpostin lähetyksen testaus ilman osoitepalvelua

* https://localhost:8080/viestintapalvelu-ui/#/reportMessages/list
* https://localhost:8080/viestintapalvelu-ui/#/reportLetters
* https://localhost:8080/viestintapalvelu-ui/#/letter-templates (ei vielä käytössä 30.1.2015)

* http://localhost:8080/ryhmasahkoposti-service/swagger/index.html
* http://localhost:8080/viestintapalvelu/swagger/index.html

# Ajastettuja prosesseja

Tsekkaa spring/application-context.xml:t

* ryhmasahkoposti: EmailServiceImpl, DailyTaskRunnerImpl
* viestintapalvelu: ServiceRecoveryHandler

# AWS-juttuja

Palvelu voi käyttää amazonin S3 palvelua tallentamaan pdf-tiedostoja. 
Ominaisuus otetaan käyttöön suorittamalla palvelu "aws"-spring profiililla. 
Profiili annetaan komentoriviparametrina tyyliin `-Dspring.profiles.active=aws`.
Palvelu tallentaa tiedostot palvelimelle jos profiilia ei käytetä. AWS-profiili vaatii toimiakseen toimivat
AWS tunnarit. [Dokumentaatio](http://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/credentials.html)
