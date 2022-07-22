<?php
    include('/usr/share/php/Net/DNS2.php');
//    ini_set('log_errors', TRUE);
//    ini_set('error_log', 'error.log');

    $resolver = new Net_DNS2_Resolver(array('nameservers' => array('2606:4700:4700::1111')));
    $mojang_ip = $resolver->query("sessionserver.mojang.com.", 'A')->answer[0]->address;
    $url = 'https://' . $mojang_ip . '/session/minecraft/';
    $accounts = json_decode(file_get_contents('accounts.json'), true);
    $sessions = json_decode(file_get_contents('sessions.json'), true);

    $post_data = file_get_contents('php://input');
    $json_data = json_decode($post_data, true);
    $uuid = $json_data['selectedProfile'];
    $server_hash = $json_data['serverId'];
    if (isset($json_data['authString'])) {
        $auth_string = $json_data['authString'];
        $f = fopen('testfile.txt', 'w');
        fwrite($f, json_encode($auth_string));
        fclose($f);
        if (isset($accounts[$uuid]) && $accounts[$uuid] == $auth_string) {
            $ch = curl_init();
            curl_setopt($ch, CURLOPT_URL, $url . 'profile/' . $uuid);
            curl_setopt($ch, CURLOPT_HTTPHEADER, array('Host: sessionserver.mojang.com'));
            curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);
            curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
            curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
            curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "GET");
            $profile = json_decode(curl_exec($ch), true);
            curl_close($ch);
            if (isset($profile['name'])) {
                $username = strtolower($profile['name']);
                if (isset($sessions[$username]) && count($sessions[$username]) > 0) {
                    foreach (array_keys($sessions[$username]) as $session_hash) {
                        if ($sessions[$username][$session_hash] < time() - 60) {
                            unset($sessions[$username][$session_hash]);
                        }
                    }
                }
                $sessions[$username]['uuid'] = $uuid;
                $sessions[$username][$server_hash] = time();
                $f = fopen('sessions.json', 'w');
                fwrite($f, json_encode($sessions));
                fclose($f);
                http_response_code(204);
            } else {
                http_response_code(503);
            }
        } else {
            http_response_code(401);
        }
        exit();
    }
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url . 'join');
    curl_setopt($ch, CURLOPT_HTTPHEADER, array('Host: sessionserver.mojang.com'));
    curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $post_data);
    curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));
    $result = curl_exec($ch);
    http_response_code(curl_getinfo($ch, CURLINFO_HTTP_CODE));
    curl_close($ch);
?>
