/**
 * Copyright (c) 2009-2011 Sindice Limited. All Rights Reserved.
 *
 * Project and contact information: http://www.siren.sindice.com/
 *
 * This file is part of the SIREn project.
 *
 * SIREn is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * SIREn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with SIREn. If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * @project siren-core_rdelbru
 * @author Campinas Stephane [ 21 Sep 2011 ]
 * @link stephane.campinas@deri.org
 */
package org.sindice.siren.search;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoringRewrite;
import org.sindice.siren.search.SirenBooleanClause.Occur;
import org.sindice.siren.search.SirenMultiTermQuery.RewriteMethod;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Class copied from {@link ScoringRewrite} for the siren use case
 */
public abstract class SirenScoringRewrite<Q extends Query> extends SirenTermCollectingRewrite<Q> {

  /** A rewrite method that first translates each term into
   *  {@link BooleanClause.Occur#SHOULD} clause in a
   *  BooleanQuery, and keeps the scores as computed by the
   *  query.  Note that typically such scores are
   *  meaningless to the user, and require non-trivial CPU
   *  to compute, so it's almost always better to use {@link
   *  MultiTermQuery#CONSTANT_SCORE_AUTO_REWRITE_DEFAULT} instead.
   *
   *  <p><b>NOTE</b>: This rewrite method will hit {@link
   *  BooleanQuery.TooManyClauses} if the number of terms
   *  exceeds {@link BooleanQuery#getMaxClauseCount}.
   *
   *  @see #setRewriteMethod */
  public final static SirenScoringRewrite<SirenBooleanQuery> SCORING_BOOLEAN_QUERY_REWRITE = new SirenScoringRewrite<SirenBooleanQuery>() {
    @Override
    protected SirenBooleanQuery getTopLevelQuery() {
      return new SirenBooleanQuery(true);
    }
    
    @Override
    protected void addClause(SirenBooleanQuery topLevel, Term term, float boost) {
      final SirenTermQuery tq = new SirenTermQuery(term);
      tq.setBoost(boost);
      topLevel.add(tq, Occur.SHOULD);
    }
    
    // Make sure we are still a singleton even after deserializing
    protected Object readResolve() {
      return SCORING_BOOLEAN_QUERY_REWRITE;
    }    
  };
  
  /** Like {@link #SCORING_BOOLEAN_QUERY_REWRITE} except
   *  scores are not computed.  Instead, each matching
   *  document receives a constant score equal to the
   *  query's boost.
   * 
   *  <p><b>NOTE</b>: This rewrite method will hit {@link
   *  BooleanQuery.TooManyClauses} if the number of terms
   *  exceeds {@link BooleanQuery#getMaxClauseCount}.
   *
   *  @see #setRewriteMethod */
  public final static RewriteMethod CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE = new RewriteMethod() {
    @Override
    public Query rewrite(IndexReader reader, SirenMultiTermQuery query) throws IOException {
      final SirenBooleanQuery bq = SCORING_BOOLEAN_QUERY_REWRITE.rewrite(reader, query);
      // TODO: if empty boolean query return NullQuery?
      if (bq.clauses().isEmpty())
        return bq;
      // strip the scores off
//      final Query result = new ConstantScoreQuery(bq);
//      result.setBoost(query.getBoost());
//      return result;
      throw new NotImplementedException();
    }

    // Make sure we are still a singleton even after deserializing
    protected Object readResolve() {
      return CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE;
    }
  };

  @Override
  public Q rewrite(final IndexReader reader, final SirenMultiTermQuery query) throws IOException {
    final Q result = getTopLevelQuery();
    final int[] size = new int[1]; // "trick" to be able to make it final
    collectTerms(reader, query, new TermCollector() {
      public boolean collect(Term t, float boost) throws IOException {
        addClause(result, t, query.getBoost() * boost);
        size[0]++;
        return true;
      }
    });
    query.incTotalNumberOfTerms(size[0]);
    return result;
  }
  
}
