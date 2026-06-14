package server.communication;

public enum ResponseStatus {
    // generici
    OK(0),
    GAME_CHANGING(1),
    NOT_LOGGED(2),
    USER_NOT_FOUND(3),
    WRONG_PASSWORD(4),
    USERNAME_TAKEN(5),
    USERNAME_BANNED(6),

    // login
    LOGIN_ALREADY_CONNECTED(10),

    //update credentials
    UPDATE_ACTIVE_USER(20),

    //submit proposal
    PROPOSAL_WRONG_SIZE(30),
    PROPOSAL_UNKNOWN_WORD(31),
    PROPOSAL_ALREADY_GROUPED(32),
    PROPOSAL_REPEATED_WORD(33),
    PROPOSAL_OLD_GAME(34),
    PROPOSAL_ALREADY_PLAYED(35),

    //game info
    INFO_GAME_NOT_PLAYED(40),

    //game stats
    STATS_GAME_NOT_FOUND(50);

    private final int code;

    ResponseStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}