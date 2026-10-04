package dev.ijlal.stacks.data

// Sample books so an admin can fill an empty library with one tap.
object SampleBooks {
    val all = listOf(
        // Fashion
        BookInput(
            title = "Gods and Kings",
            author = "Dana Thomas",
            isbn = "9781594204944",
            category = "Fashion",
            publishedYear = 2015,
            description = "How two outsiders, Lee McQueen and John Galliano, reshaped luxury fashion, " +
                "and what the industry took from them in return.",
            totalCopies = 3,
        ),
        BookInput(
            title = "Alexander McQueen: Savage Beauty",
            author = "Andrew Bolton",
            isbn = "9781588394125",
            category = "Fashion",
            publishedYear = 2011,
            description = "The catalogue of the Met's McQueen retrospective: romantic, brutal and " +
                "theatrical collections, photographed piece by piece.",
            totalCopies = 2,
        ),
        BookInput(
            title = "Rei Kawakubo / Comme des Garçons: Art of the In-Between",
            author = "Andrew Bolton",
            isbn = "9781588396204",
            category = "Fashion",
            publishedYear = 2017,
            description = "Comme des Garçons read through its in-betweens: absence and presence, " +
                "design and not-design, fashion and anti-fashion.",
            totalCopies = 1,
        ),
        BookInput(
            title = "The Fashion System",
            author = "Roland Barthes",
            isbn = "9780520071773",
            category = "Fashion",
            publishedYear = 1967,
            description = "Barthes reads fashion magazines as a language, decoding how words turn " +
                "clothes into meaning.",
            totalCopies = 2,
        ),
        BookInput(
            title = "Women in Clothes",
            author = "Sheila Heti, Heidi Julavits & Leanne Shapton",
            isbn = "9780399166563",
            category = "Fashion",
            publishedYear = 2014,
            description = "Hundreds of women from around the world on what they wear, why they wear it " +
                "and what it says about them.",
            totalCopies = 3,
        ),
        // Music
        BookInput(
            title = "Just Kids",
            author = "Patti Smith",
            isbn = "9780060936228",
            category = "Music",
            publishedYear = 2010,
            description = "Patti Smith's memoir of late-sixties New York and her bond with Robert " +
                "Mapplethorpe, before either of them was famous.",
            totalCopies = 4,
        ),
        BookInput(
            title = "How Music Works",
            author = "David Byrne",
            isbn = "9781936365531",
            category = "Music",
            publishedYear = 2012,
            description = "David Byrne on how rooms, technology and money shape the music we make " +
                "and the way we hear it.",
            totalCopies = 3,
        ),
        BookInput(
            title = "Rip It Up and Start Again",
            author = "Simon Reynolds",
            isbn = "9780143036722",
            category = "Music",
            publishedYear = 2005,
            description = "The restless, experimental years after punk, from Joy Division and " +
                "Talking Heads to the birth of synth-pop.",
            totalCopies = 2,
        ),
        BookInput(
            title = "Chronicles: Volume One",
            author = "Bob Dylan",
            isbn = "9780743244589",
            category = "Music",
            publishedYear = 2004,
            description = "Dylan's own account of arriving in Greenwich Village and the records that " +
                "followed, told out of order.",
            totalCopies = 2,
        ),
        BookInput(
            title = "Lords of Chaos",
            author = "Michael Moynihan & Didrik Søderlind",
            isbn = "9780922915941",
            category = "Music",
            publishedYear = 1998,
            description = "A history of the Norwegian black metal underground and the violence that " +
                "grew around it.",
            totalCopies = 1,
        ),
        BookInput(
            title = "Life",
            author = "Keith Richards",
            isbn = "9780316034418",
            category = "Music",
            publishedYear = 2010,
            description = "Keith Richards on the blues, the Rolling Stones and five decades of excess, " +
                "in his own unfiltered voice.",
            totalCopies = 2,
        ),
        // Art
        BookInput(
            title = "Ways of Seeing",
            author = "John Berger",
            isbn = "9780140135152",
            category = "Art",
            publishedYear = 1972,
            description = "A short, radical book on how images, from oil paintings to adverts, teach " +
                "us how to look.",
            totalCopies = 4,
        ),
        BookInput(
            title = "The Story of Art",
            author = "E. H. Gombrich",
            isbn = "9780714832470",
            category = "Art",
            publishedYear = 1950,
            description = "The classic one-volume history of art, from cave paintings to the modern era.",
            totalCopies = 2,
        ),
        BookInput(
            title = "Interaction of Color",
            author = "Josef Albers",
            isbn = "9780300179354",
            category = "Art",
            publishedYear = 1963,
            description = "Albers' hands-on course in how colours change one another, and why our " +
                "eyes can't be trusted.",
            totalCopies = 2,
        ),
        BookInput(
            title = "On Photography",
            author = "Susan Sontag",
            isbn = "9780312420093",
            category = "Art",
            publishedYear = 1977,
            description = "Essays on how the camera changed the way we see the world, and ourselves.",
            totalCopies = 3,
        ),
        BookInput(
            title = "Steal Like an Artist",
            author = "Austin Kleon",
            isbn = "9780761169253",
            category = "Art",
            publishedYear = 2012,
            description = "Ten short lessons on creativity, influence and getting your work out " +
                "into the world.",
            totalCopies = 3,
        ),
        // Design
        BookInput(
            title = "The Design of Everyday Things",
            author = "Don Norman",
            isbn = "9780465050659",
            category = "Design",
            publishedYear = 2013,
            description = "Why some doors are impossible to open, and what that teaches us about " +
                "designing things people can actually use.",
            totalCopies = 2,
        ),
    )
}
