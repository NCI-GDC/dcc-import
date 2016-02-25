/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.imports.gene.reader;

import java.util.HashMap;
import java.util.Map;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads display names for genes and constructs map of symbol to gene name
 */
@Slf4j
public final class NameReader {

  /**
   * Constants
   */
  private static final String XREF_URI =
      "ftp://ftp.ensembl.org/pub/grch37/release-82/mysql/homo_sapiens_core_82_37/xref.txt.gz";

  /**
   * Caching
   */
  public static Map<String, String> entrezMap = new HashMap<String, String>();
  public static Map<String, String> hgncMap = new HashMap<String, String>();
  public static Map<String, String> mimMap = new HashMap<String, String>();
  public static Map<String, String> uniprotMap = new HashMap<String, String>();

  /**
   * Get the map of xref display id -> gene name Caches external db ids in hashmaps for entrez, hgnc, mim, & uniprot.
   */
  public static Map<String, String> readXrefDisplay() {
    log.info("Reading xref table for gene names and caching external db ids");

    val retMap = new HashMap<String, String>();
    BaseReader.read(XREF_URI, (String[] line) -> {
      // Only use the rows we care about.
      if ("12600".equals(line[1])) {
        // Gene Wiki
        String symbol = line[3];
        String name = line[5];
        retMap.put(symbol, name);
      } else if ("1300".equals(line[1])) {
        // Entrez
        String xrefId = line[0];
        String dbID = line[2];
        entrezMap.put(xrefId, dbID);
      } else if ("1100".equals(line[1])) {
        // HGNC
        String xrefId = line[0];
        String dbID = line[2];
        hgncMap.put(xrefId, dbID);
      } else if ("1510".equals(line[1])) {
        // MIM_GENE
        String xrefId = line[0];
        String dbID = line[2];
        mimMap.put(xrefId, dbID);
      } else if ("2200".equals(line[1])) {
        // Uniprot/SWISSPROT
        String xrefId = line[0];
        String dbID = line[2];
        uniprotMap.put(xrefId, dbID);
      }
    });

    return retMap;
  }

}
