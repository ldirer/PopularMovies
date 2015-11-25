Popular Movies
==============

This app uses data from The Movie Database (TMDb) but is not endorsed by TMDb.


## Building the project with your API Key
Data displayed in the app is courtesy of the Movie Database.
To build the project with your api key, include a file `app.properties` file at the same level as the
build.gradle file with `API_KEY=your_key` as content.


## Implementation notes

The sorting functionality of the movie database API seems to be broken (when asking for a popularity.desc
order we get vaguely sorted results, as if they were sorted then locally shuffled).

We store the data in a sqlite database and we include the order of the movies we get from the movie database API, to provide a consistent order to users.
