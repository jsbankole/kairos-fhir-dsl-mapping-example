package projects.gecco.crf.labVital

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.FlexiStudy
import de.kairos.fhir.centraxx.metamodel.LaborFindingLaborValue
import de.kairos.fhir.centraxx.metamodel.LaborValue
import de.kairos.fhir.centraxx.metamodel.StudyMember
import org.hl7.fhir.r4.model.Observation

import static de.kairos.fhir.centraxx.metamodel.RootEntities.laborMapping
import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/sarscov2igaserpliaacnc
 * @author Lukas Reinert, Mike Wähnert
 * @since KAIROS-FHIR-DSL.v.1.9.0, CXX.v.3.18.1.7
 *
 * hints:
 *  A StudyEpisode is no regular episode and cannot reference an encounter
 */

observation {
  final def studyMember = context.source[laborMapping().relatedPatient().studyMembers()].find {
    it[StudyMember.STUDY][FlexiStudy.CODE] == "GECCO FINAL"
  }
  if (!studyMember) {
    return //no export
  }

  final def profileName = context.source[laborMapping().laborFinding().laborMethod().code()]
  if (profileName != "COV_GECOO_LABOR") {
    return //no export
  }

  final def labValAb = context.source[laborMapping().laborFinding().laborFindingLaborValues()].find {
    "COV_GECCO_SARS_COV_2_IGG_IA" == it[LaborFindingLaborValue.LABOR_VALUE][LaborValue.CODE]
  }
  if (!labValAb) {
    return
  }

  def numVal =labValAb[LaborFindingLaborValue.NUMERIC_VALUE]
  if (!numVal){
    return
  }

  id = "Observation/SARSCoV2-IGG-IA-" + context.source[laborMapping().id()]

  meta {
    source = "https://fhir.centraxx.de"
    profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/sars-cov-2-iga-ser-pl-ia-acnc"
  }

  // TODO check identifier
  identifier {
    type {
      coding {
        system = "http://terminology.hl7.org/CodeSystem/v2-0203"
        code = "OBI"
      }
    }
    system = "https://www.charite.de/fhir/CodeSystem/lab-identifiers"
    value = "94504-8_SARS-CoV-2-Ab-Panel"
    assigner {
      reference = "Organization/Charité"
    }
  }

  status = Observation.ObservationStatus.UNKNOWN

  category {
    coding {
      system = "http://loinc.org"
      code = "26436-6"
    }
    coding {
      system = "http://terminology.hl7.org/CodeSystem/observation-category"
      code = "laboratory"
    }
  }

  code {
    coding {
      system = "http://loinc.org"
      code = "94720-0"
      display = "SARS-CoV-2 (COVID-19) IgA Ab [Units/volume] in Serum or Plasma by Immunoassay"
    }
    text = "SARS-CoV-2 IgA antibodies quantitative"
  }

  effectiveDateTime {
    date = normalizeDate(context.source[laborMapping().creationDate()] as String)
    precision = TemporalPrecisionEnum.DAY.toString()
  }

  subject {
    reference = "Patient/Patient-" + context.source[laborMapping().relatedPatient().idContainer()]["psn"][0]
  }

  valueQuantity {
    value = numVal
    unit = "[IU]/mL"
    system = "http://unitsofmeasure.org"
    code = "[IU]/mL"
  }
}


static String normalizeDate(final String dateTimeString) {
  return dateTimeString != null ? dateTimeString.substring(0, 19) : null
}