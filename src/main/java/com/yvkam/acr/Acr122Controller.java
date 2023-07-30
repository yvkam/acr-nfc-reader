package com.yvkam.acr;

import com.yvkam.acr.core.Card;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.yvkam.acr.Acr122CLI.readCardUidOnce;


@RestController
@RequiredArgsConstructor
public class Acr122Controller {


    @GetMapping("/acr122/read")
    @CrossOrigin
    public Card getAll() {
        Card card = new Card();
        card.setUid(readCardUidOnce());
        return card;
    }
    
}
