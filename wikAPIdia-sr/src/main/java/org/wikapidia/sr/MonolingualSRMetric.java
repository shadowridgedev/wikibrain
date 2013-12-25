package org.wikapidia.sr;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.normalize.Normalizer;

import java.io.IOException;

/**
 * A monolingual SR metric supports SR operations in a single language.
 * @author Matt Lesicko
 * @author Shilad Sen
 */

public interface MonolingualSRMetric {

    /**
     * @return the name of the similarity metric in a human readable format
     */
    public String getName();

    /**
     * @return The language associated with this metric.
     */
    public Language getLanguage();

    /**
     * Determine the similarity between two local pages.
     *
     * @param pageId1 Id of the first page.
     * @param pageId2 Id of the second page.
     * @param explanations Whether explanations should be created.
     * @return
     */
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException;

    /**
     * Determine the similarity between two strings in a given language by mapping through local pages.
     *
     * @param phrase1 The first phrase.
     * @param phrase2 The second phrase.
     * @param explanations Whether explanations should be created.
     * @return
     */
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException;

    /**
     * Find the most similar local pages to a local page within the same language.
     *
     * @param pageId The id of the local page whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @return
     */
    public SRResultList mostSimilar(int pageId, int maxResults) throws DaoException;

    /**
     * Find the most similar local pages to a local page.
     *
     * @param pageId The id of the local page whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param validIds The local page ids to be considered.  Null means all ids in the language.
     * @return
     */
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException;

    /**
     * Find the most similar local pages to a phrase.
     *
     * @param phrase The phrase whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @return
     */
    public SRResultList mostSimilar(String phrase, int maxResults) throws DaoException;

    /**
     * Find the most similar local pages to a phrase.
     *
     * @param phrase The phrase whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param validIds The local page ids to be considered.  Null means all ids in the language
     * @return
     */
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException;

    /**
     * Writes the metric to a directory.
     *
     * @param path A directory data will be written to.
     *                  Any existing data in the directory may be destroyed.
     * @throws java.io.IOException
     */
    public void write(String path) throws IOException;

    /**
     * Reads the metric from a directory.
     *
     * @param path A directory data will be read from.
     *                  The directory previously will have been written to by write().
     * @throws java.io.IOException if the file is not found or is unusable
     */
    public void read(String path) throws IOException;

    /**
     * Train the similarity() function.
     * The KnownSims may already be associated with Wikipedia ids (check wpId1 and wpId2).
     *
     * @param dataset A gold standard dataset
     */
    public void trainSimilarity(Dataset dataset) throws DaoException;

    /**
     * Train the mostSimilar() function
     * The KnownSims may already be associated with Wikipedia ids (check wpId1 and wpId2).
     *
     * @param dataset A gold standard dataset.
     * @param numResults The maximum number of similar articles computed per phrase.
     * @param validIds The Wikipedia ids that should be considered in result sets. Null means all ids.
     */
    public void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds);

    /**
     * @return true if similarity() is already trained (or doesn't need training)
     */
    public boolean similarityIsTrained();

    /**
     * @return true if mostSimilar() is already trained (or doesn't need training)
     */
    public boolean mostSimilarIsTrained();


    public void setMostSimilarNormalizer(Normalizer n);

    public void setSimilarityNormalizer(Normalizer defaultSimilarityNormalizer);

    /**
     * Return a vector representation of a LocalPage
     * @param id Local id of the page to be described.
     * @return A vector of a page's scores versus all other pages
     */
    public TIntDoubleMap getVector(int id) throws DaoException;


    /**
     * Construct a cosimilarity matrix of Wikipedia ids in a given language.
     *
     * @param wpRowIds
     * @param wpColIds
     * @return
     * @throws java.io.IOException
     */
    public double[][] cosimilarity(int wpRowIds[], int wpColIds[]) throws DaoException;


    /**
     * Construct a cosimilarity matrix of phrases.
     *
     * @param rowPhrases
     * @param colPhrases
     * @return
     * @throws java.io.IOException
     */
    public double[][] cosimilarity(String rowPhrases[], String colPhrases[]) throws DaoException;

    /**
     * Construct symmetric comsimilarity matrix of Wikipedia ids in a given language.
     *
     * @param ids
     * @return
     * @throws java.io.IOException
     */
    public double[][] cosimilarity(int ids[]) throws DaoException;

    /**
     * Construct symmetric cosimilarity matrix of phrases by mapping through local pages.
     *
     * @param phrases
     * @return
     * @throws java.io.IOException
     */
    public double[][] cosimilarity(String phrases[]) throws DaoException;

    /**
     * Writes a cosimilarity matrix to file based off of the getVector function and pairwise cosine similarity class.
     * This should be considered a "cache" that speeds up underlying cosimilarity calculations.
     *
     * @param path the directory to write the matrix in
     * @param maxHits the number of document hits you would like returned from the most similar function
     */
    public void writeCosimilarity(String path, int maxHits) throws IOException, DaoException, WikapidiaException;

    /**
     * Writes a cosimilarity matrix to file based off of the getVector function and pairwise cosine similarity class.
     * This should be considered a "cache" that speeds up underlying cosimilarity calculations.
     *
     * @param path the directory to write the matrix in
     * @param maxHits the number of document hits you would like returned from the most similar function
     * @param rowIds The page ids that will be cached
     * @param colIds The page ids that can be returned in the resulting similarity lists
     */
    public void writeCosimilarity(String path, int maxHits, TIntSet rowIds, TIntSet colIds) throws IOException, DaoException, WikapidiaException;

    public void readCosimilarity(String path) throws IOException;

    Normalizer getMostSimilarNormalizer();

    Normalizer getSimilarityNormalizer();
}