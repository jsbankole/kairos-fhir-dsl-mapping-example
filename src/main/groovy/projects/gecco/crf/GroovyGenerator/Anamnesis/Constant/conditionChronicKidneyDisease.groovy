package projects.gecco.crf

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue

import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/chronickidneydiseases
 * @author Lukas Reinert, Mike Wähnert
 * @since KAIROS-FHIR-DSL.v.1.8.0, CXX.v.3.18.1
 */
condition {
    final def studyCode = context.source[studyVisitItem().studyMember().study().code()]
    if (studyCode != "GECCO FINAL") {
        return //no export
    }
    final def crfName = context.source[studyVisitItem().template().crfTemplate().name()]
    final def studyVisitStatus = context.source[studyVisitItem().status()]
    if (crfName != "SarsCov2_ANAMNESE / RISIKOFAKTOREN" || studyVisitStatus != "APPROVED") {
        return //no export
    }

    final def crfItemKidney = context.source[studyVisitItem().crf().items()].find {
        "COV_GECCO_NIERENERKRANKUNG" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
    }
    if (crfItemKidney[CrfItem.CATALOG_ENTRY_VALUE] == []) {
        return
    }

    final def VERcode = crfItemKidney[CrfItem.CATALOG_ENTRY_VALUE][0][CatalogEntry.CODE]
    def VERcodeSeverity = ""

    final def crfItemKidneySeverity = context.source[studyVisitItem().crf().items()].find {
        "COV_GECCO_NIERENERKRANKUNG_SCHWEREGRAD" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
    }

    if(VERcode == "COV_JA"){

        if (crfItemKidneySeverity[CrfItem.CATALOG_ENTRY_VALUE] == []) {
            return
        }

        VERcodeSeverity = crfItemKidneySeverity[CrfItem.CATALOG_ENTRY_VALUE][0][CatalogEntry.CODE] as String
    }

    id = "Condition/ChronicKidneyDisease-" + context.source[studyVisitItem().crf().id()]

    meta {
        source = "https://fhir.centraxx.de"
        profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/chronic-kidney-diseases"
    }

    if (VERcode == "COV_JA") {
        clinicalStatus {
            coding{
                system = "http://terminology.hl7.org/CodeSystem/condition-clinical"
                code = "active"
                display = "Active"
            }
        }

        verificationStatus {
            coding {
                system = "http://terminology.hl7.org/CodeSystem/condition-ver-status"
                code = "confirmed"
                display = "Confirmed"
            }
            coding {
                system = "http://snomed.info/sct"
                code = "410605003"
                display = "Confirmed present (qualifier value)"
            }
        }

    } else if (VERcode == "COV_NEIN") {
        verificationStatus {
            coding {
                system = "http://terminology.hl7.org/CodeSystem/condition-ver-status"
                code = "refuted"
                display = "Refuted"
            }
            coding {
                system = "http://snomed.info/sct"
                code = "410594000"
                display = "Definitely NOT present (qualifier value)"
            }
        }

    } else {
        modifierExtension {
            url = "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/uncertainty-of-presence"
            valueCodeableConcept {
                coding {
                    system = "http://snomed.info/sct"
                    code = "261665006"
                    display = "Unknown (qualifier value)"
                }
                text = "Presence of condition is unknown."
            }
        }
    }

    category {
        coding {
            system = "http://snomed.info/sct"
            code = "394589003"
            display = "Nephrology (qualifier value)"
        }
    }

    code{
        final def ICDcode = matchResponseToICD(VERcodeSeverity)
        coding {
            system = "http://fhir.de/CodeSystem/dimdi/icd-10-gm"
            code = ICDcode[0]
            display = ICDcode[1]
        }

        final def SNOMEDcode = matchResponseToSNOMED(VERcodeSeverity)
        coding {
            system = "http://snomed.info/sct"
            code = SNOMEDcode[0]
            display = SNOMEDcode[1]
        }
    }

    subject {
        reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
    }

    recordedDate {
        date = normalizeDate(crfItemKidney[CrfItem.CREATIONDATE] as String)
        precision = TemporalPrecisionEnum.DAY.toString()
    }
}

static String[] matchResponseToICD(final String resp) {
    switch (resp) {
        case ("COV_GECCO_NIERENERKRANKUNG_SCHWEREGRADE_1"):
            return ["N18.1", "Chronische Nierenkrankheit, Stadium 1"]
        case ("COV_GECCO_NIERENERKRANKUNG_SCHWEREGRADE_2"):
            return ["N18.2", "Chronische Nierenkrankheit, Stadium 2"]
        case ("COV_GECCO_NIERENERKRANKUNG_SCHWEREGRADE_3"):
            return ["N18.3", "Chronische Nierenkrankheit, Stadium 3"]
        case ("COV_GECCO_NIERENERKRANKUNG_SCHWEREGRADE_4"):
            return ["N18.4", "Chronische Nierenkrankheit, Stadium 4"]
        case ("COV_GECCO_NIERENERKRANKUNG_SCHWEREGRADE_5"):
            return ["N18.5", "Chronische Nierenkrankheit, Stadium 5"]
        case ("COV_GECCO_NIERENERKRANKUNG_SCHWEREGRADE_5_DIALYSIS"):
            return ["Z99.2", "Langzeitige Abhängigkeit von Dialyse bei Niereninsuffizienz"]
        default: ["N18.9", "Chronische Nierenkrankheit, nicht näher bezeichnet"]
    }
}

static String[] matchResponseToSNOMED(final String resp) {
    switch (resp) {
        case ("COV_GECCO_NIERENERKRANKUNG_SCHWEREGRADE_1"):
            return ["431855005", "Chronic kidney disease stage 1 (disorder)"]
        case ("COV_GECCO_NIERENERKRANKUNG_SCHWEREGRADE_2"):
            return ["431856006", "Chronic kidney disease stage 2 (disorder)"]
        case ("COV_GECCO_NIERENERKRANKUNG_SCHWEREGRADE_3"):
            return ["433144002", "Chronic kidney disease stage 3 (disorder)"]
        case ("COV_GECCO_NIERENERKRANKUNG_SCHWEREGRADE_4"):
            return ["431857002", "Chronic kidney disease stage 4 (disorder)"]
        case ("COV_GECCO_NIERENERKRANKUNG_SCHWEREGRADE_5"):
            return ["433146000", "Chronic kidney disease stage 5 (disorder)"]
        case ("COV_GECCO_NIERENERKRANKUNG_SCHWEREGRADE_5_DIALYSIS"):
            return ["714152005", "Chronic kidney disease stage 5 on dialysis (disorder)"]
        default: ["709044004", "Chronic kidney disease (disorder)"]
    }
}

static String normalizeDate(final String dateTimeString) {
    return dateTimeString != null ? dateTimeString.substring(0, 10) : null
}
