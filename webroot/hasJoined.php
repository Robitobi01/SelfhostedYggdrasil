<?php
//    ini_set('log_errors', TRUE);
//    ini_set('error_log', 'error.log');

    // The system resolver must return Mojang's real public address. dns_get_record()
    // queries DNS directly and does not consult /etc/hosts.
    $mojang_ip = null;
    $records = @dns_get_record('sessionserver.mojang.com.', DNS_A);
    if (is_array($records)) {
        foreach ($records as $record) {
            if (isset($record['type'], $record['ip'])
                && $record['type'] === 'A'
                && filter_var($record['ip'], FILTER_VALIDATE_IP, FILTER_FLAG_IPV4) !== false) {
                $mojang_ip = $record['ip'];
                break;
            }
        }
    }
    if ($mojang_ip === null) {
        http_response_code(502);
        exit;
    }
    $url = 'https://' . $mojang_ip . '/session/minecraft/';
    $sessions = json_decode(file_get_contents('sessions.json'), true);

    $username = strtolower($_GET['username']);
    $server_hash = $_GET['serverId'];

    if (isset($sessions[$username][$server_hash]) && $sessions[$username][$server_hash] >= time() - 60) {
        $uuid = $sessions[$username]['uuid'];
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url . "profile/" . $uuid . '?unsigned=false');
        curl_setopt($ch, CURLOPT_HTTPHEADER, array('Host: sessionserver.mojang.com'));
        curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "GET");
        $profile = curl_exec($ch);
        echo $profile;
        http_response_code(curl_getinfo($ch, CURLINFO_HTTP_CODE));
        curl_close($ch);
    } else {
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url . 'hasJoined?username=' . $username . '&serverId=' . $server_hash);
        curl_setopt($ch, CURLOPT_HTTPHEADER, array('Host: sessionserver.mojang.com'));
        curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "GET");
        $profile = curl_exec($ch);
        echo $profile;
        http_response_code(curl_getinfo($ch, CURLINFO_HTTP_CODE));
        curl_close($ch);
    }
?>
