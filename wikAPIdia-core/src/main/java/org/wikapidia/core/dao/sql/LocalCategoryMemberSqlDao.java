package org.wikapidia.core.dao.sql;

import com.typesafe.config.Config;
import org.jooq.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalCategoryMember;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.LocalCategory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 *
 * A SQL database implementation of the LocalCategoryMemberDao.
 *
 * @author Ari Weiland
 *
 */
public class LocalCategoryMemberSqlDao extends AbstractSqlDao<LocalCategoryMember> implements LocalCategoryMemberDao {

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.CATEGORY_MEMBERS.LANG_ID,
            Tables.CATEGORY_MEMBERS.CATEGORY_ID,
            Tables.CATEGORY_MEMBERS.ARTICLE_ID,
    };
    private final LocalCategoryDao localCategoryDao;
    private final LocalArticleDao localArticleDao;

    public LocalCategoryMemberSqlDao(WpDataSource dataSource, LocalCategoryDao localCategoryDao, LocalArticleDao localArticleDao) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/category-members");
        this.localCategoryDao = localCategoryDao;
        this.localArticleDao = localArticleDao;
    }

    @Override
    public void save(LocalCategoryMember member) throws DaoException {
        insert(
                member.getLanguage().getId(),
                member.getCategoryId(),
                member.getArticleId()
        );
    }

    @Override
    public void save(LocalCategory category, LocalArticle article) throws DaoException, WikapidiaException {
        save(new LocalCategoryMember(category, article));
    }

    /**
     * This method should generally not be used.
     * @param daoFilter a set of filters to limit the search
     * @return
     * @throws DaoException
     */
    @Override
    public Iterable<LocalCategoryMember> get(DaoFilter daoFilter) throws DaoException {
        DSLContext context = getJooq();
        try {
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getLangIds() != null) {
                conditions.add(Tables.CATEGORY_MEMBERS.LANG_ID.in(daoFilter.getLangIds()));
            }
            Cursor<Record> result = context.select().
                    from(Tables.CATEGORY_MEMBERS).
                    where(conditions).
                    limit(daoFilter.getLimitOrInfinity()).
                    fetchLazy(getFetchSize());
            return new SimpleSqlDaoIterable<LocalCategoryMember>(result, context) {
                @Override
                public LocalCategoryMember transform(Record r) {
                    return buildLocalCategoryMember(r);
                }
            };
        } catch (RuntimeException e) {
            freeJooq(context);
            throw e;
        }
    }

    @Override
    public int getCount(DaoFilter daoFilter) throws DaoException{
        DSLContext context = getJooq();
        try {
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getLangIds() != null) {
                conditions.add(Tables.CATEGORY_MEMBERS.LANG_ID.in(daoFilter.getLangIds()));
            }
            return context.selectCount().
                    from(Tables.CATEGORY_MEMBERS).
                    where(conditions).
                    fetchOne().value1();
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public Collection<Integer> getCategoryMemberIds(Language language, int categoryId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Result<Record> result = context.select().
                    from(Tables.CATEGORY_MEMBERS).
                    where(Tables.CATEGORY_MEMBERS.CATEGORY_ID.eq(categoryId)).
                    and(Tables.CATEGORY_MEMBERS.LANG_ID.eq(language.getId())).
                    fetch();
            return extractIds(result, false);
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public Collection<Integer> getCategoryMemberIds(LocalCategory localCategory) throws DaoException {
        return getCategoryMemberIds(localCategory.getLanguage(), localCategory.getLocalId());
    }

    @Override
    public Map<Integer, LocalArticle> getCategoryMembers(Language language, int categoryId) throws DaoException {
        Collection<Integer> articleIds = getCategoryMemberIds(language, categoryId);
        return localArticleDao.getByIds(language, articleIds);
    }

    @Override
    public Map<Integer, LocalArticle> getCategoryMembers(LocalCategory localCategory) throws DaoException {
        Collection<Integer> articleIds = getCategoryMemberIds(localCategory);
        return localArticleDao.getByIds(localCategory.getLanguage(), articleIds);
    }

    @Override
    public Collection<Integer> getCategoryIds(Language language, int articleId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Result<Record> result = context.select().
                    from(Tables.CATEGORY_MEMBERS).
                    where(Tables.CATEGORY_MEMBERS.ARTICLE_ID.eq(articleId)).
                    and(Tables.CATEGORY_MEMBERS.LANG_ID.eq(language.getId())).
                    fetch();
            return extractIds(result, true);
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public Collection<Integer> getCategoryIds(LocalArticle localArticle) throws DaoException {
        return getCategoryIds(localArticle.getLanguage(), localArticle.getLocalId());
    }

    @Override
    public Map<Integer, LocalCategory> getCategories(Language language, int articleId) throws DaoException {
        Collection<Integer> categoryIds = getCategoryIds(language, articleId);
        return localCategoryDao.getByIds(language, categoryIds);
    }

    @Override
    public Map<Integer, LocalCategory> getCategories(LocalArticle localArticle) throws DaoException {
        Collection<Integer> categoryIds = getCategoryIds(localArticle);
        return localCategoryDao.getByIds(localArticle.getLanguage(), categoryIds);
    }

    private Collection<Integer> extractIds(Result<Record> result, boolean categoryIds) {
        if (result.isEmpty()) {
            return null;
        }
        Collection<Integer> pageIds = new ArrayList<Integer>();
        for(Record record : result) {
            pageIds.add(categoryIds ?
                    record.getValue(Tables.CATEGORY_MEMBERS.CATEGORY_ID) :
                    record.getValue(Tables.CATEGORY_MEMBERS.ARTICLE_ID)
            );
        }
        return pageIds;
    }

    private LocalCategoryMember buildLocalCategoryMember(Record r) {
        return new LocalCategoryMember(
                r.getValue(Tables.CATEGORY_MEMBERS.CATEGORY_ID),
                r.getValue(Tables.CATEGORY_MEMBERS.ARTICLE_ID),
                Language.getById(r.getValue(Tables.CATEGORY_MEMBERS.LANG_ID))
        );
    }

    public static class Provider extends org.wikapidia.conf.Provider<LocalCategoryMemberDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LocalCategoryMemberDao.class;
        }

        @Override
        public String getPath() {
            return "dao.localCategoryMember";
        }

        @Override
        public LocalCategoryMemberDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new LocalCategoryMemberSqlDao(
                        getConfigurator().get(
                                WpDataSource.class,
                                config.getString("dataSource")),
                        getConfigurator().get(LocalCategoryDao.class),
                        getConfigurator().get(LocalArticleDao.class)
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
