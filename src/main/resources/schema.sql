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
    DELETE FROM `delete_account_request` WHERE `active` IS NOT NULL AND `active` < NOW();
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

-- Create system project and its statuses
INSERT INTO `counter_sequence`(`id`, `counter_value`) VALUES (1, 0) ON DUPLICATE KEY UPDATE `id` = `id` ^;
INSERT INTO `counter_sequence`(`id`, `counter_value`) VALUES (2, 0) ON DUPLICATE KEY UPDATE `id` = `id` ^;

INSERT INTO `project` (`id`, `active`, `description`, `name`, `logo_id`, `sprint_counter_id`, `task_counter_id`) VALUES (1, NULL, '', 'Vernite - maintenance', NULL, 1, 2) ON DUPLICATE KEY UPDATE `id` = `id` ^;

INSERT INTO `status` (`id`, `color`, `is_begin`, `is_final`, `name`, `ordinal`, `project_id`) VALUES (1, 1, 1, 0, 'To do', 0, 1) ON DUPLICATE KEY UPDATE `id` = `id` ^;
INSERT INTO `status` (`id`, `color`, `is_begin`, `is_final`, `name`, `ordinal`, `project_id`) VALUES (2, 2, 0, 0, 'In progress', 1, 1) ON DUPLICATE KEY UPDATE `id` = `id` ^;
INSERT INTO `status` (`id`, `color`, `is_begin`, `is_final`, `name`, `ordinal`, `project_id`) VALUES (3, 3, 0, 1, 'Done', 2, 1) ON DUPLICATE KEY UPDATE `id` = `id` ^;
