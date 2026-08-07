package net.sinistersky.j2ee.support;

public class JsonPath {

    Expression parseExpression(String str) throws JsonPathException {
        return new Parser().parseExpression(str);
    }
}
