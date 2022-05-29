DROP PROCEDURE IF EXISTS `counter_increment` ^;

CREATE PROCEDURE `counter_increment` (IN `counter_id` int, OUT `result` int)
BEGIN
    START TRANSACTION;
    UPDATE `counter_sequence` `cs` SET `cs`.`counter_value` = `cs`.`counter_value` + 1 WHERE `cs`.`id` = `counter_id`;
    SELECT `counter_value` INTO `result` from `counter_sequence` WHERE `id` = `counter_id`;
    COMMIT;
END ^;

DROP EVENT IF EXISTS `remove_old_content` ^;
CREATE EVENT `remove_old_content` ON SCHEDULE EVERY 1 MINUTE ON COMPLETION NOT PRESERVE ENABLE DO
BEGIN
    DELETE FROM `delete_account` WHERE `active` IS NOT NULL AND `active` < NOW();
    DELETE FROM `password_recovery` WHERE `active` IS NOT NULL AND `active` < NOW();
    DELETE FROM `git_hub_integration` WHERE `active` IS NOT NULL AND `active` < NOW();
    DELETE FROM `git_hub_task` WHERE `active` IS NOT NULL AND `active` < NOW();
    DELETE FROM `project` WHERE `active` IS NOT NULL AND `active` < NOW();
    DELETE FROM `status` WHERE `active` IS NOT NULL AND `active` < NOW();
    DELETE FROM `status` WHERE `active` IS NOT NULL AND `active` < NOW();
    DELETE FROM `task` WHERE `active` IS NOT NULL AND `active` < NOW();
    DELETE FROM `user_session` WHERE `last_used` < DATE_SUB(NOW(), INTERVAL 30 MINUTE) AND `remembered` = 0;
    -- userów trzeba inaczej
    DELETE FROM `workspace` WHERE `active` IS NOT NULL AND `active` < NOW();
END ^;
