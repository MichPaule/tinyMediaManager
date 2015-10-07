/*
 * Copyright 2012 - 2015 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.ui.movies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.MovieTrailer;
import org.tinymediamanager.scraper.MediaArtwork;
import org.tinymediamanager.scraper.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.MediaLanguages;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScrapeOptions;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.MediaTrailer;
import org.tinymediamanager.scraper.MediaType;
import org.tinymediamanager.scraper.mediaprovider.IMovieArtworkProvider;
import org.tinymediamanager.scraper.mediaprovider.IMovieMetadataProvider;
import org.tinymediamanager.scraper.mediaprovider.IMovieTrailerProvider;
import org.tinymediamanager.ui.UTF8Control;

/**
 * The Class MovieChooserModel.
 * 
 * @author Manuel Laggner
 */
public class MovieChooserModel extends AbstractModelObject {
  private static final ResourceBundle   BUNDLE           = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$
  private static final Logger           LOGGER           = LoggerFactory.getLogger(MovieChooserModel.class);
  public static final MovieChooserModel emptyResult      = new MovieChooserModel();

  private MediaScraper                  metadataProvider = null;
  private List<MediaScraper>            artworkScrapers  = null;
  private List<MediaScraper>            trailerScrapers  = null;

  private MediaLanguages                language         = null;
  private MediaSearchResult             result           = null;
  private MediaMetadata                 metadata         = null;
  private String                        name             = "";
  private String                        overview         = "";
  private String                        year             = "";
  private String                        combinedName     = "";
  private String                        posterUrl        = "";
  private String                        tagline          = "";
  private boolean                       scraped          = false;

  public MovieChooserModel(MediaScraper metadataProvider, List<MediaScraper> artworkScrapers, List<MediaScraper> trailerScrapers,
      MediaSearchResult result, MediaLanguages language) {
    this.metadataProvider = metadataProvider;
    this.artworkScrapers = artworkScrapers;
    this.trailerScrapers = trailerScrapers;
    this.result = result;
    this.language = language;

    // name
    setName(result.getTitle());
    // year
    setYear(result.getYear());
    // combined name (name (year))
    setCombinedName();
  }

  /**
   * create the empty search result.
   */
  private MovieChooserModel() {
    setName(BUNDLE.getString("chooser.nothingfound")); //$NON-NLS-1$
    combinedName = name;
  }

  public void setName(String name) {
    String oldValue = this.name;
    this.name = name;
    firePropertyChange("name", oldValue, name);
  }

  public void setOverview(String overview) {
    String oldValue = this.overview;
    this.overview = overview;
    firePropertyChange("overview", oldValue, overview);
  }

  public String getName() {
    return name;
  }

  public String getOverview() {
    return overview;
  }

  public String getPosterUrl() {
    return posterUrl;
  }

  public void setPosterUrl(String newValue) {
    String oldValue = posterUrl;
    posterUrl = newValue;
    firePropertyChange("posterUrl", oldValue, newValue);
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    String oldValue = this.year;
    this.year = year;
    firePropertyChange("year", oldValue, year);
  }

  public void setCombinedName() {
    String oldValue = this.combinedName;
    this.combinedName = getName() + " (" + getYear() + ")";
    firePropertyChange("combinedName", oldValue, this.combinedName);
  }

  public String getCombinedName() {
    return combinedName;
  }

  /**
   * Scrape meta data.
   */
  public void scrapeMetaData() {
    try {
      // poster for preview
      setPosterUrl(result.getPosterUrl());

      MediaScrapeOptions options = new MediaScrapeOptions(MediaType.MOVIE);
      options.setResult(result);
      options.setLanguage(language);
      options.setCountry(MovieModuleManager.MOVIE_SETTINGS.getCertificationCountry());
      metadata = ((IMovieMetadataProvider) metadataProvider.getMediaProvider()).getMetadata(options);
      setOverview(metadata.getStringValue(MediaMetadata.PLOT));
      setTagline(metadata.getStringValue(MediaMetadata.TAGLINE));

      if (StringUtils.isBlank(posterUrl) && StringUtils.isNotBlank(metadata.getStringValue(MediaMetadata.POSTER_URL))) {
        setPosterUrl(metadata.getStringValue(MediaMetadata.POSTER_URL));
      }

      scraped = true;

    }
    catch (IOException e) {
      LOGGER.error("scrapeMedia", e);
      MessageManager.instance.pushMessage(
          new Message(MessageLevel.ERROR, "MovieChooser", "message.scrape.threadcrashed", new String[] { ":", e.getLocalizedMessage() }));
    }
    catch (Exception e) {
      LOGGER.error("scrapeMedia", e);
      MessageManager.instance.pushMessage(
          new Message(MessageLevel.ERROR, "MovieChooser", "message.scrape.threadcrashed", new String[] { ":", e.getLocalizedMessage() }));
    }
  }

  public List<MediaArtwork> getArtwork() {
    List<MediaArtwork> artwork = new ArrayList<MediaArtwork>();

    if (!scraped) {
      return artwork;
    }

    MediaScrapeOptions options = new MediaScrapeOptions(MediaType.MOVIE);
    options.setArtworkType(MediaArtworkType.ALL);
    options.setMetadata(metadata);
    options.setId(MediaMetadata.IMDB, String.valueOf(metadata.getId(MediaMetadata.IMDB)));
    try {
      options.setTmdbId(Integer.parseInt(String.valueOf(metadata.getId(MediaMetadata.TMDB))));
    }
    catch (Exception e) {
      options.setTmdbId(0);
    }
    options.setLanguage(language);
    options.setCountry(MovieModuleManager.MOVIE_SETTINGS.getCertificationCountry());

    // scrape providers till one artwork has been found
    for (MediaScraper artworkScraper : artworkScrapers) {
      IMovieArtworkProvider artworkProvider = (IMovieArtworkProvider) artworkScraper.getMediaProvider();
      try {
        artwork.addAll(artworkProvider.getArtwork(options));
      }
      catch (Exception e) {
      }
    }

    // at last take the poster from the result
    if (StringUtils.isNotBlank(getPosterUrl())) {
      MediaArtwork ma = new MediaArtwork();
      ma.setType(MediaArtworkType.POSTER);
      ma.setDefaultUrl(getPosterUrl());
      ma.setProviderId(result.getProviderId());
      artwork.add(ma);
    }

    return artwork;
  }

  public List<MovieTrailer> getTrailers() {
    List<MovieTrailer> trailers = new ArrayList<MovieTrailer>();

    if (!scraped) {
      return trailers;
    }

    MediaScrapeOptions options = new MediaScrapeOptions(MediaType.MOVIE);
    options.setMetadata(metadata);
    options.setId(MediaMetadata.IMDB, String.valueOf(metadata.getId(MediaMetadata.IMDB)));
    try {
      options.setTmdbId(Integer.parseInt(String.valueOf(metadata.getId(MediaMetadata.TMDB))));
    }
    catch (Exception e) {
      options.setTmdbId(0);
    }
    options.setLanguage(language);
    options.setCountry(MovieModuleManager.MOVIE_SETTINGS.getCertificationCountry());

    // scrape trailers
    for (MediaScraper trailerScraper : trailerScrapers) {
      try {
        IMovieTrailerProvider trailerProvider = (IMovieTrailerProvider) trailerScraper.getMediaProvider();
        List<MediaTrailer> foundTrailers = trailerProvider.getTrailers(options);
        for (MediaTrailer mediaTrailer : foundTrailers) {
          MovieTrailer movieTrailer = new MovieTrailer(mediaTrailer);
          trailers.add(movieTrailer);
        }
      }
      catch (Exception e) {
        LOGGER.warn(e.getMessage());
      }
    }

    return trailers;
  }

  public MediaMetadata getMetadata() {
    return metadata;
  }

  public boolean isScraped() {
    return scraped;
  }

  public void setTagline(String newValue) {
    String oldValue = this.tagline;
    this.tagline = newValue;
    firePropertyChange("tagline", oldValue, newValue);
  }

  public String getTagline() {
    return tagline;
  }

}
