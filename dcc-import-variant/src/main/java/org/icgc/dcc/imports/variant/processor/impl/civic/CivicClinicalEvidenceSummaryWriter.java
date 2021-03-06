package org.icgc.dcc.imports.variant.processor.impl.civic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.Observable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.icgc.dcc.imports.variant.model.CivicClinicalEvidenceSummary;
import org.icgc.dcc.imports.variant.processor.api.ContentWriter;
import org.jongo.Jongo;
import org.jongo.MongoCollection;


/**
 * Copyright (c) 2017 The Ontario Institute for Cancer Research. All rights reserved.
 * <p>
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 * <p>
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

@RequiredArgsConstructor
public class CivicClinicalEvidenceSummaryWriter implements ContentWriter<CivicClinicalEvidenceSummary>{

  @NonNull private Jongo jongo;
  @NonNull private String collectionName;
  private ObjectMapper mapper = new ObjectMapper();

  public void cleanCollection() {
    jongo.getCollection(collectionName).drop();
  }

  @Override
  public Observable<Object> write(Observable<CivicClinicalEvidenceSummary> instance) {
    this.cleanCollection();
    MongoCollection collection = jongo.getCollection(collectionName);
    return
      instance.map(item ->
        collection.insert( mapper.convertValue(item, ObjectNode.class) )
      );
  }
}
