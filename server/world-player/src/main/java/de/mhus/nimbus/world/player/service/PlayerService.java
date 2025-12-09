package de.mhus.nimbus.world.player.service;

import de.mhus.nimbus.world.player.session.SessionPingConsumer;
import de.mhus.nimbus.world.player.session.PlayerSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PlayerService implements SessionPingConsumer {

    @Override
    public ACTION onSessionPing(PlayerSession session) {
        return null;
    }
}
