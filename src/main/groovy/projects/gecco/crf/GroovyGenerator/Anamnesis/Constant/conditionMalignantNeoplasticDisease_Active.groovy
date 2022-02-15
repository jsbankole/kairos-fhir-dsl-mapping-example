package projects.gecco.crf.GroovyGenerator.Anamnesis.Constant

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue

import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem
/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/malignantneoplasticdisease
 * @author Lukas Reinert, Mike Wähnert
 * @since KAIROS-FHIR-DSL.v.1.8.0, CXX.v.3.18.1
 *
 */


condition {
  final def studyCode = context.source[studyVisitItem().studyMember().study().code()]
  if (studyCode != "GECCO FINAL"){
    return //no export
  }
  final def crfName = context.source[studyVisitItem().template().crfTemplate().name()]
  final def studyVisitStatus = context.source[studyVisitItem().status()]
  if (crfName != "SarsCov2_ANAMNESE / RISIKOFAKTOREN" || studyVisitStatus == "OPEN") {
    return //no export
  }
  final def crfItemCancer = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_TUMORERKRANKUNG_ACTIVE" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (!crfItemCancer){
    return
  }
  if (crfItemCancer[CrfItem.CATALOG_ENTRY_VALUE] != []) {
    id = "Condition/MalignantNeoplasticDiseaseActive-" + context.source[studyVisitItem().crf().id()]

    meta {
      source = "https://fhir.centraxx.de"
      profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/malignant-neoplastic-disease"
    }

    crfItemCancer[CrfItem.CATALOG_ENTRY_VALUE]?.each { final item ->
      final def statusCode = item[CatalogEntry.CODE] as String

      if (statusCode == "COV_JA") {
        clinicalStatus {
          coding {
            system = "http://terminology.hl7.org/CodeSystem/condition-clinical"
            code = "active"
            display = "Active"
          }
        }

        verificationStatus {
          coding {
            system = "http://snomed.info/sct"
            code = "410605003"
            display = "Confirmed present (qualifier value)"
          }
          coding {
            system = "http://terminology.hl7.org/CodeSystem/condition-ver-status"
            code = "confirmed"
            display = "Confirmed"
          }
        }

      } else if(statusCode == "COV_NEIN"){
        verificationStatus {
          coding {
            system = "http://snomed.info/sct"
            code = "410594000"
            display = "Definitely NOT present (qualifier value)"
          }
          coding {
            system = "http://terminology.hl7.org/CodeSystem/condition-ver-status"
            code = "refuted"
            display = "Refuted"
          }
        }

      } else if(statusCode == "COV_UNBEKANNT"){
        // Unbekannt
        extension {
          url = "https://simplifier.net/forschungsnetzcovid-19/uncertaintyofpresence"
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
    }

    category {
      coding {
        system = "http://snomed.info/sct"
        code = "394593009"
        display = "Medical oncology (qualifier value)"
      }
    }

    subject {
      reference = "Patient/Patient-" + context.source[studyVisitItem().studyMember().patientContainer().idContainer()]?.find {"MPI" == it["idContainerType"]?.getAt("code")}["psn"]
    }

    code {
      coding {
        system = "http://snomed.info/sct"
        code = "363346000"
        display = "Malignant neoplastic disease"
      }
    }

    recordedDate {
      date = normalizeDate(crfItemCancer[CrfItem.CREATIONDATE] as String)
      precision = TemporalPrecisionEnum.DAY.toString()
    }
  }
}

static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 10) : null
}
