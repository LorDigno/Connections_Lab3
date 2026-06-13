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

    //submit proposal
    PROPOSAL_WRONG_SIZE(20),
    PROPOSAL_UNKNOWN_WORD(21),
    PROPOSAL_ALREADY_GROUPED(22),
    PROPOSAL_WRONG(23),
    PROPOSAL_CORRECT(24),
    PROPOSAL_REPEATED_WORD(25),
    PROPOSAL_OLD_GAME(26),
    PROPOSAL_ALREADY_PLAYED(27);

    private final int code;

    ResponseStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}