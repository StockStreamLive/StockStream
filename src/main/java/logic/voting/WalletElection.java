package logic.voting;

import data.factory.CommandFactory;
import logic.wallet.WalletEngine;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.data.WalletCommand;
import stockstream.logic.elections.Election;

import javax.annotation.PostConstruct;

public class WalletElection extends Election<WalletCommand> {

    @Autowired
    private CommandFactory commandFactory;

    @Autowired
    private WalletEngine walletEngine;

    public WalletElection() {
        super("#wallet", WalletCommand.class, 3);
    }

    @PostConstruct
    public void init() {
        this.withMessageParser(m -> commandFactory.constructWalletCommand(m))
            .withInstantElection((walletCommand, voter) -> walletEngine.processWalletCommandFromVoter(walletCommand, voter));
    }

}
