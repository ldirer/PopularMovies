Popular Movies
==============


## Implementation
We query the Movie Database once every time the user wants to sort the movies differently.
We store information about the movies but not their sorting order.
When accessing a detail page, we fetch the movie data from the database.

The sorting functionality of the movie database API seems to be broken (when asking for a popularity.desc
order we get vaguely sorted results, as if they were sorted then locally shuffled).
