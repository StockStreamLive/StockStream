package logic.voting;

import data.Vote;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import stockstream.data.Voter;
import stockstream.database.ElectionRegistry;
import stockstream.logic.Scheduler;
import stockstream.twitch.TwitchAPI;
import stockstream.twitch.TwitchChat;

import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class VoteEngineTest {

    @Mock
    private TwitchAPI twitchAPI;

    @Mock
    private TwitchChat twitchChat;

    @Mock
    private Scheduler scheduler;

    @Mock
    private SpeedElection speedElection;

    @Mock
    private WalletElection walletElection;

    @Mock
    private TradingElection tradingElection;

    @Mock
    private ElectionRegistry electionRegistry;

    @InjectMocks
    private VoteEngine voteEngine;

    @Before
    public void setTest() {
        MockitoAnnotations.initMocks(this);
        voteEngine.init();
    }

    @Test
    public void multithreadedPoll_oneVote_expectVoteProcessed() {

        when(tradingElection.getExpirationDate()).thenReturn(DateUtils.addHours(new Date(), 1).getTime());
        when(speedElection.getExpirationDate()).thenReturn(DateUtils.addHours(new Date(), 1).getTime());
        when(walletElection.getExpirationDate()).thenReturn(DateUtils.addHours(new Date(), 1).getTime());

        when(tradingElection.receiveVote(any(), any())).thenReturn(Optional.empty());
        when(speedElection.receiveVote(any(), any())).thenReturn(Optional.empty());
        when(walletElection.receiveVote(any(), any())).thenReturn(Optional.empty());

        voteEngine.enqueueVote(new Vote(new Voter("mike", "twitch", "#stockstream", true), "!test", "#stockstream", new Date().getTime()));

        voteEngine.multiThreadedPoll();

        assertEquals(0, voteEngine.getVoteQueue().size());
    }

    @Test
    public void multithreadedPoll_oneVoteElectionExpired_expectVoteRequeued() {

        when(tradingElection.getExpirationDate()).thenReturn(DateUtils.addHours(new Date(), -1).getTime());
        when(speedElection.getExpirationDate()).thenReturn(DateUtils.addHours(new Date(), -1).getTime());
        when(walletElection.getExpirationDate()).thenReturn(DateUtils.addHours(new Date(), -1).getTime());

        when(tradingElection.receiveVote(any(), any())).thenReturn(Optional.empty());
        when(speedElection.receiveVote(any(), any())).thenReturn(Optional.empty());
        when(walletElection.receiveVote(any(), any())).thenReturn(Optional.empty());

        voteEngine.enqueueVote(new Vote(new Voter("mike", "twitch", "#stockstream", true), "!test", "#stockstream", new Date().getTime()));

        voteEngine.multiThreadedPoll();

        assertEquals(1, voteEngine.getVoteQueue().size());
    }

}
