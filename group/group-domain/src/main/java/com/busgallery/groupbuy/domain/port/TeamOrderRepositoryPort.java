package com.busgallery.groupbuy.domain.port;

import com.busgallery.groupbuy.domain.model.TeamOrderAggregate;

/**
 * Repository port for team order.
 */
public interface TeamOrderRepositoryPort {

    /**
     * Find team order by team id.
     *
     * @param teamId team id
     * @return team aggregate
     */
    TeamOrderAggregate findByTeamId(String teamId);

    /**
     * Save team order aggregate.
     *
     * @param aggregate team aggregate
     */
    void save(TeamOrderAggregate aggregate);
}
