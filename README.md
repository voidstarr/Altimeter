# Altimeter

### [Discord](https://discord.gg/eeZbM9umBy)

Sponge API 7.3.x Plugin that limits the number of accounts that can connect from an IP address

##The gist
A list exists per IP, each list can hold up to X accounts and each account will be removed from the queue until the TTL expires.
If the list fills for that IP, no more accounts can be logged in from that IP.

## Definitions
* TTL: `Time to live`

## Commands
* `/altimeter`: lists sub commands
* `/altimeter clear [all|x.x.x.x]`: clear the all account list, or the account list associated with a given IP address (v4/v6)
* `/altimeter override x.x.x.x <limit>`: set account limit for `x.x.x.x` to `limit`

## Permissions
* `altimeter.override`
* `altimeter.clear.ip`
* `altimeter.clear.all`

## Configuration
```
altimeter {
    # How many accounts from one IP can log in.
    accountLimit=5
    # How often accounts should be checked and cleared from IP lists.
    checkInterval {
        # DAYS, HOURS, MINUTES, or SECONDS
        unit=MINUTES
        value=5
    }
    # Override account limit for specific IPs
    limitOverrides=[
        {
            ip="127.0.0.1"
            limit=5
        }
    ]
    # How long after an account is added to the queue is it removed.
    ttl {
        # DAYS, HOURS, MINUTES, or SECONDS
        unit=DAYS
        value=30
    }
}
```