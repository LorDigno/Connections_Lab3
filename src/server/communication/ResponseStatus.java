package server.communication;

public enum ResponseStatus {
    // generici
    OK(0),
    GAME_CHANGING(1),

    // login
    LOGIN_USER_NOT_FOUND(10),
    LOGIN_WRONG_PASSWORD(11),
    LOGIN_ALREADY_CONNECTED(12),

    //logout
    LOGOUT_NOT_LOGGED(20),

    //registrazione
    REGISTER_USERNAME_TAKEN(30),
    REGISTER_USERNAME_BANNED(31),

    //updateCredentials
    UPDATE_USER_NOT_FOUND(40),
    UPDATE_WRONG_PASSWORD(41),
    UPDATE_USERNAME_TAKEN(42),
    UPDATE_USERNAME_BANNED(43),


    //partita
    PROPOSAL_WRONG_SIZE(50),
    PROPOSAL_UNKNOWN_WORD(51),
    PROPOSAL_ALREADY_GROUPED(52),
    PROPOSAL_WRONG(53),
    PROPOSAL_CORRECT(54),
    PROPOSAL_REPEATED_WORD(55);

    private final int code;

    ResponseStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}