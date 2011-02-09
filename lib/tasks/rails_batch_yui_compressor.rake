 desc "YUI Compressor(batchuicompressor) , Compress JavaScript and CSS"
 task :yui_compress do
   puts `cd vendor/plugins/rails_batch_yui_compressor/lib/batchyuicompressor/ ; java -jar batchyuicompressor.jar`
 end