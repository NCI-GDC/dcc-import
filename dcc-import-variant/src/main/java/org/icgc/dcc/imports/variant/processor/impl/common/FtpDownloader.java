package org.icgc.dcc.imports.variant.processor.impl.common;

import io.reactivex.Observable;
import it.sauronsoftware.ftp4j.FTPClient;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icgc.dcc.imports.variant.processor.api.Downloader;

import java.io.File;

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

@Slf4j
@RequiredArgsConstructor
public class FtpDownloader implements Downloader {

  @NonNull
  private String host;
  @NonNull
  private String path;
  @NonNull
  private String remoteFilename;
  @NonNull
  private String localFullpath;

  @Override
  public Observable<File> download() {

    return
      Observable.just(new FTPClient()).map(ftp -> {

        log.info("Downloading the file at " + localFullpath);

        File localFile = new File(localFullpath);
        ftp.connect(host);
        ftp.changeDirectory(path);
        ftp.setType(FTPClient.TYPE_BINARY);
        ftp.download(remoteFilename, localFile);

        return localFile;

      });

  }
}
