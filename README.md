# jwt-clojure-example

This example may helps who starting develop web api by clojure.

## Situation

Basically, this example assume OAuth2.0 resource owner password credential flow.
Please see detail > [https://tools.ietf.org/html/rfc6749#section-4.3](https://tools.ietf.org/html/rfc6749#section-4.3)

 - You are developing your service. Of course, there is user and attribute may includes username, email and password.
 - For create user, send get request to /ticket endpoint, and get ticket. This ticket is only for create new user.
 - You can post new user(username, email, password) with ticket.

 - After create user, you may want to login, so let's get token by username, password.
 -- returns token (optional refresh_token) so you attach this token to your each requests.
 - get user info by token

## You may have to write
 - fix database system for persistent.
 - write logic about refresh token, for save and verify it if you want to use refresh_token.

## Testing src code

You can run autotest as follows:

    lein ring server
    (require 'midje.repl) (midje.repl/autotest)

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run with swagger document:

    SWAGGER=1 lein ring server

## License

Copyright © 2014 vimtaku
