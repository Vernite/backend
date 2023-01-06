-- Create system project and its statuses
INSERT INTO `counter_sequence`(`id`, `counter_value`) VALUES (1, 0) ON DUPLICATE KEY UPDATE `id` = `id` ^;
INSERT INTO `counter_sequence`(`id`, `counter_value`) VALUES (2, 0) ON DUPLICATE KEY UPDATE `id` = `id` ^;

INSERT INTO `project` (`id`, `active`, `description`, `name`, `logo_id`, `sprint_counter_id`, `task_counter_id`) VALUES (1, NULL, '', 'Vernite - maintenance', NULL, 1, 2) ON DUPLICATE KEY UPDATE `id` = `id` ^;

INSERT INTO `status` (`id`, `color`, `is_begin`, `is_final`, `name`, `ordinal`, `project_id`) VALUES (1, 1, 1, 0, 'To do', 0, 1) ON DUPLICATE KEY UPDATE `id` = `id` ^;
INSERT INTO `status` (`id`, `color`, `is_begin`, `is_final`, `name`, `ordinal`, `project_id`) VALUES (2, 2, 0, 0, 'In progress', 1, 1) ON DUPLICATE KEY UPDATE `id` = `id` ^;
INSERT INTO `status` (`id`, `color`, `is_begin`, `is_final`, `name`, `ordinal`, `project_id`) VALUES (3, 3, 0, 1, 'Done', 2, 1) ON DUPLICATE KEY UPDATE `id` = `id` ^;
