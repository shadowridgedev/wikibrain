package org.wikapidia.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.fi.FinnishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.FinnishStemmer;
import org.wikapidia.core.lang.Language;
import org.wikapidia.lucene.TokenizerOptions;

/**
 * @author Ari Weiland
 */
public class FinnishTokenizer extends LanguageTokenizer {

    protected FinnishTokenizer(Version version, TokenizerOptions options, Language language) {
        super(version, options, language);
    }

    @Override
    public TokenStream getTokenStream(TokenStream input, CharArraySet stemExclusionSet) {
        TokenStream stream = new StandardFilter(matchVersion, input);
        if (caseInsensitive)
            stream = new LowerCaseFilter(matchVersion, stream);
        if (useStopWords)
            stream = new StopFilter(matchVersion, stream, FinnishAnalyzer.getDefaultStopSet());
        if (useStem) {
            if (!stemExclusionSet.isEmpty())
                stream = new SetKeywordMarkerFilter(stream, stemExclusionSet);
            stream = new SnowballFilter(stream, new FinnishStemmer());
        }
        return stream;
    }
}