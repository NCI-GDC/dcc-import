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
package org.icgc.dcc.imports.project.writer;

import static org.icgc.dcc.common.core.model.ReleaseCollection.PROJECT_COLLECTION;

import org.icgc.dcc.imports.core.util.AbstractJongoWriter;
import org.icgc.dcc.imports.project.model.Project;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.jongo.MongoCollection;

import com.mongodb.MongoClientURI;

@Slf4j
public class ProjectWriter extends AbstractJongoWriter<Iterable<Project>> {

  public ProjectWriter(MongoClientURI mongoUri) {
    super(mongoUri);
  }

  @Override
  public void writeValue(Iterable<Project> projects) {
    log.info("Clearing project documents...");
    val projectCollection = getCollection(PROJECT_COLLECTION);
    clearProjects(projectCollection);

    for (val project : projects) {
      log.info("Writing project {} ...", project.get__project_id());
      projectCollection.save(project);
    }
  }

  private static void clearProjects(MongoCollection projectCollection) {
    val result = projectCollection.remove();
    log.info("Finished clearing target collection {}: {}", projectCollection, result);
  }

}
