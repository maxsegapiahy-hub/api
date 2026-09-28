CREATE TABLE `affiliates` (
  `id` int AUTO_INCREMENT NOT NULL,
  `userId` int NOT NULL,
  `referralCode` varchar(24) NOT NULL,
  `status` enum('pending','active','suspended') NOT NULL DEFAULT 'active',
  `createdAt` timestamp NOT NULL DEFAULT (now()),
  `updatedAt` timestamp NOT NULL DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `affiliates_id` PRIMARY KEY(`id`),
  CONSTRAINT `affiliates_userId_unique` UNIQUE(`userId`),
  CONSTRAINT `affiliates_referralCode_unique` UNIQUE(`referralCode`)
);
--> statement-breakpoint
CREATE TABLE `affiliate_pillars` (
  `id` int AUTO_INCREMENT NOT NULL,
  `affiliateId` int NOT NULL,
  `pillar` enum('maxseg','max_saude','max_beneficios') NOT NULL,
  `createdAt` timestamp NOT NULL DEFAULT (now()),
  CONSTRAINT `affiliate_pillars_id` PRIMARY KEY(`id`),
  CONSTRAINT `affiliate_pillars_affiliate_pillar` UNIQUE(`affiliateId`,`pillar`)
);
