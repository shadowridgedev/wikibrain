package org.wikapidia.core.dao.sql;

import com.typesafe.config.Config;
import gnu.trove.map.hash.TLongIntHashMap;
import org.apache.commons.io.IOUtils;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class LocalPageSqlDao<T extends LocalPage> extends AbstractSqlDao implements LocalPageDao<T> {
    private TLongIntHashMap titlesToIds = null;

    public LocalPageSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    @Override
    public void beginLoad() throws DaoException {
        Connection conn=null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            LocalPageSqlDao.class.getResource("/db/local-page-schema.sql")
                    ));
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public void save(LocalPage page) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            context.insertInto(Tables.LOCAL_PAGE).values(
                    null,
                    page.getLanguage().getId(),
                    page.getLocalId(),
                    page.getTitle().getCanonicalTitle(),
                    page.getNameSpace().getArbitraryId(),
                    page.isRedirect()
            ).execute();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public void endLoad() throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            LocalPageSqlDao.class.getResource("/db/local-page-indexes.sql")
                    ));
            if (cache!=null){
                cache.updateTableLastModified("page_table");
            }
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public T getById(Language language, int pageId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.PAGE_ID.eq(pageId)).
                    and(Tables.LOCAL_PAGE.LANG_ID.eq(language.getId())).
                    fetchOne();
            return (T)buildLocalPage(record);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public T getByTitle(Language language, Title title, NameSpace nameSpace) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.TITLE.eq(title.getCanonicalTitle())).
                    and(Tables.LOCAL_PAGE.LANG_ID.eq(title.getLanguage().getId())).
                    and(Tables.LOCAL_PAGE.NAME_SPACE.eq(nameSpace.getArbitraryId())).
                    fetchOne();
            return (T)buildLocalPage(record);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public Map<Integer, T> getByIds(Language language, Collection<Integer> pageIds) throws DaoException {
        if (pageIds == null || pageIds.isEmpty()) {
            return null;
        }
        Map<Integer, T> map = new HashMap<Integer, T>();
        for (Integer pageId : pageIds){
            map.put(pageId, getById(language, pageId));
        }
        return map;
    }

    @Override
    public Map<Title, T> getByTitles(Language language, Collection<Title> titles, NameSpace nameSpace) throws DaoException {
        if (titles == null || titles.isEmpty()) {
            return null;
        }
        Map<Title, T> map = new HashMap<Title, T>();
        for (Title title : titles){
            map.put(title, getByTitle(language, title, nameSpace));
        }
        return map;
    }

    public int getIdByTitle(String title, Language language, NameSpace nameSpace) throws DaoException {
        if (titlesToIds==null){
            titlesToIds=buildTitlesToIds();
        }
        return titlesToIds.get(hashTitle(title,language.getId(),nameSpace.ordinal()));
    }

    /**
     * Build a LocalPage from a database record representation.
     * Classes that extend class this should override this method.
     *
     * @param record a database record
     * @return a LocalPage representation of the given database record
     * @throws DaoException if the record is not a Page
     */
    protected LocalPage buildLocalPage(Record record) throws DaoException {
        if (record == null) {
            return null;
        }
        Language lang = Language.getById(record.getValue(Tables.LOCAL_PAGE.LANG_ID));
        Title title = new Title(
                record.getValue(Tables.LOCAL_PAGE.TITLE), true,
                LanguageInfo.getByLanguage(lang));
        NameSpace nameSpace = NameSpace.getNameSpaceById(record.getValue(Tables.LOCAL_PAGE.NAME_SPACE));
        return new LocalPage(
                lang,
                record.getValue(Tables.LOCAL_PAGE.PAGE_ID),
                title,
                nameSpace
        );
    }

    /**
     *
     * @param s The canonical title of the page.
     * @param lang_id
     * @param page_type_id
     * @return
     */
    protected long hashTitle(String s, int lang_id, int page_type_id){
        s = s+lang_id+page_type_id;
        long h = 1125899906842597L; //prime
        int len = s.length();
        for (int i = 0; i < len; i++) {
            h = 31*h + s.charAt(i);
        }
        return h;
    }

    protected long hashTitle(Title title, Language language, NameSpace nameSpace){
        return hashTitle(title.getCanonicalTitle(),language.getId(),nameSpace.ordinal());
    }

    protected TLongIntHashMap buildTitlesToIds() throws DaoException {
        Connection conn = null;
        try {
            if (cache!=null){
                TLongIntHashMap map = (TLongIntHashMap)cache.get("titlesToIds","local_page");
                if (map!=null){
                    return map;
                }
            }
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Cursor<Record> cursor = context.select().
                    from(Tables.LOCAL_PAGE).
                    fetchLazy();
            TLongIntHashMap map = new TLongIntHashMap();
            for (Record record : cursor){
                long hash = hashTitle(record.getValue(Tables.LOCAL_PAGE.TITLE),
                        record.getValue(Tables.LOCAL_PAGE.LANG_ID),
                        record.getValue(Tables.LOCAL_PAGE.NAME_SPACE));
                map.put(hash, record.getValue(Tables.LOCAL_PAGE.PAGE_ID));
            }
            if (cache!=null){
                cache.saveToCache("titlesToIds",map,"page_table");
            }
            return map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    public static class Provider extends org.wikapidia.conf.Provider<LocalPageDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LocalPageDao.class;
        }

        @Override
        public String getPath() {
            return "dao.localPage";
        }

        @Override
        public LocalPageDao get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new LocalPageSqlDao(
                            getConfigurator().get(
                                DataSource.class,
                                config.getString("dataSource"))
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
