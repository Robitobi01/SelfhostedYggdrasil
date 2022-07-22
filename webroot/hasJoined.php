<?php
    include('/usr/share/php/Net/DNS2.php');
//    ini_set('log_errors', TRUE);
//    ini_set('error_log', 'error.log');

    $resolver = new Net_DNS2_Resolver(array('nameservers' => array('2606:4700:4700::1111')));
    $mojang_ip = $resolver->query("sessionserver.mojang.com.", 'A')->answer[0]->address;
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
