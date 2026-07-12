UPDATE detection_media SET file_url = CONCAT('/uploads/', SUBSTRING_INDEX(file_url, '\\', -1)) WHERE LEFT(file_url, 2) = 'c:';
SELECT id, file_url FROM detection_media;
