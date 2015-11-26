/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.imports.drug;

import java.util.List;

import org.icgc.dcc.imports.core.SourceImporter;
import org.icgc.dcc.imports.core.model.ImportSource;
import org.icgc.dcc.imports.drug.reader.DrugReader;
import org.icgc.dcc.imports.drug.reader.GeneReader;
import org.icgc.dcc.imports.drug.reader.TrialsReader;
import org.icgc.dcc.imports.drug.writer.DrugWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.MongoClientURI;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.collect.Lists.newArrayList;

@Slf4j
public class DrugImporter implements SourceImporter {

  private final static ObjectMapper MAPPER = new ObjectMapper();
  private final static List<String> GENE_FIELDS_FOR_REMOVE = newArrayList("chembl", "description", "gene_name", "name");

  @NonNull
  private final MongoClientURI mongoUri;

  public DrugImporter(@NonNull MongoClientURI mongoUri) {
    this.mongoUri = mongoUri;
  }

  @Override
  public ImportSource getSource() {
    return ImportSource.DRUGS;
  }

  @Override
  @SneakyThrows
  public void execute() {
    log.info("Getting Drug Data");
    val drugs = new DrugReader().getDrugs().readAll();
    log.info("Number of drugs to denormalize: {}", drugs.size());

    writeDrugs(readAndJoin(drugs));
  }

  /**
   * Calls all the helpers.
   */
  private List<ObjectNode> readAndJoin(List<ObjectNode> drugs) {
    return joinTrials(
        joinGenes(
            expandImageUrls(
                denormalizeAtcCodes(drugs))));
  }

  @SneakyThrows
  private void writeDrugs(List<ObjectNode> drugs) {
    val drugWriter = new DrugWriter(mongoUri);
    drugWriter.writeFiles(drugs);
    log.info("FINISHED WRITING TO MONGO");
    drugWriter.close();
  }

  /**
   * Creates nodes that contain URLs for the small and large versions of the molecule image
   */
  private List<ObjectNode> expandImageUrls(List<ObjectNode> drugs) {
    log.info("Creating image urls");

    drugs.forEach(drug -> {
      String imageUrl = drug.get("image_url").asText();
      String largeImageUrl = imageUrl.replace(".png", "-large.png");

      drug.put("small_image_url", imageUrl);
      drug.put("large_image_url", largeImageUrl);
      drug.remove("image_url");
    });

    return drugs;
  }

  /**
   * Joins Genes to Drugs by gene name. We include ensembl ids as part of gene node.
   */
  private List<ObjectNode> joinGenes(List<ObjectNode> drugs) {
    log.info("Joining Genes to Drugs");
    val geneMap = new GeneReader(mongoUri).getGeneMap();

    drugs.forEach(drug -> {
      JsonNode drugGenes = drug.get("genes");
      ArrayNode geneArray = MAPPER.createArrayNode();

      if (drugGenes.isArray()) {
        for (JsonNode geneName : drugGenes) {
          if (geneMap.containsKey(geneName.asText())) {
            ObjectNode cleanedMap = geneMap.get(geneName.asText()).remove(GENE_FIELDS_FOR_REMOVE);
            geneArray.add(cleanedMap);
          } else {
            log.warn("Gene missing on join: {}", geneName.asText());
          }
        }
      }

      drug.set("genes", geneArray);
    });

    return drugs;
  }

  /**
   * Joins trials to Drugs by trial code. Trials will be already joined with conditions.
   */
  private List<ObjectNode> joinTrials(List<ObjectNode> drugs) {
    log.info("Joining Trials to Drugs");
    val trialsMap = new TrialsReader().getTrialsMap();

    drugs.forEach(drug -> {
      JsonNode drugTrials = drug.get("trials");
      ArrayNode trialsArray = MAPPER.createArrayNode();

      if (drugTrials.isArray()) {
        for (JsonNode trialCode : drugTrials) {
          if (trialsMap.containsKey(trialCode.asText())) {
            trialsArray.add(trialsMap.get(trialCode.asText()));
          } else {
            log.warn("Trail missing on join: {}", trialCode.asText());
          }
        }
      }

      drug.put("cancer_trial_count", trialsArray.size());
      drug.set("trials", trialsArray);
    });

    return drugs;
  }
  
  /**
   * Moves level5 ATC codes into the main ATC code JSON node.
   */
  private List<ObjectNode> denormalizeAtcCodes(List<ObjectNode> drugs) {
    log.info("Denormalizing ATC Codes");

    drugs.forEach(drug -> {
      ArrayNode atcCodes = (ArrayNode) drug.get("atc_codes");
      ArrayNode level5 = (ArrayNode) drug.get("atc_level5_codes");
      if (atcCodes != null) {
        atcCodes.forEach(atc -> {
          for (JsonNode code : level5) {
            if (code.asText().indexOf(atc.get("code").asText()) >= 0) {
              ((ObjectNode) atc).put("atc_level5_codes", code.asText());
              break;
            }
          }
        });
        drug.remove("atc_level5_codes");
      } else {
        ArrayNode atcClasses = (ArrayNode) drug.get("atc_classifications");
        if (atcClasses != null) {
          ArrayNode newAtcCodes = MAPPER.createArrayNode();
          atcClasses.forEach(atcClass -> {
            ObjectNode newAtcEntry = MAPPER.createObjectNode();
            newAtcEntry.put("code", atcClass.get("level4").asText());
            newAtcEntry.put("atc_level5_codes", atcClass.get("level5").asText());
            newAtcEntry.put("description", atcClass.get("level4_description").asText());
            newAtcCodes.add(newAtcEntry);
          });
          drug.set("atc_codes", newAtcCodes);
          drug.remove("atc_classifications");
        }
      }
    });

    return drugs;
  }

}
