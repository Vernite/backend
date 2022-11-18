package dev.vernite.vernite.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.swscale;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;

public class ImageConverter {

    /**
     * converts any video/image/picture to webp. Default settings: 75% quality,
     * lossy, YUVA420P
     * 
     * @param b
     * @return byte array
     */
    public static byte[] convertImage(String filename, byte[] b) throws IOException {
        Pointer mem = null;
        AVIOContext pb = null;
        AVFormatContext ifCtx = null;
        AVCodecContext decCtx = null;
        AVFrame src = null;
        AVFrame dst = null;
        AVPacket pkt = null;
        SwsContext swsCtx = null;
        AVCodecContext encCtx = null;
        BytePointer data = null;
        try {
            mem = avutil.av_malloc(b.length + 16);
            if (mem == null || mem.address() == 0) {
                throw new IOException("Could not allocate memory");
            }
            mem.capacity(b.length + 16);
            data = new BytePointer(mem);
            // 0: cursor
            // 8: capacity
            // 16: data
            data.position(16);
            data.put(b);
            data.putLong(-16, 0); // cursor
            data.putLong(-8, b.length); // capacity
            pb = avformat.avio_alloc_context((BytePointer) null, 0, 0, mem, null, null, null);
            if (pb == null) {
                throw new IOException("Could not allocate AVIOContext");
            }
            pb.direct(1);
            pb.seekable(1);
            pb.read_packet(ReadPointer.INSTANCE);
            pb.seek(SeekPointer.INSTANCE);
            ifCtx = avformat.avformat_alloc_context();
            if (ifCtx == null) {
                throw new IOException("Could not allocate AVFormatContext");
            }
            ifCtx.pb(pb);
            if (avformat.avformat_open_input(ifCtx, filename, null, (AVDictionary) null) < 0) {
                throw new IOException("Could not open input");
            }
            int stream = avformat.av_find_best_stream(ifCtx, avutil.AVMEDIA_TYPE_VIDEO, -1, -1, (AVCodec) null, 0);
            if (stream < 0) {
                throw new IOException("Could not find video stream");
            }
            AVCodec decoder = avcodec.avcodec_find_decoder(ifCtx.streams(stream).codecpar().codec_id());
            if (decoder == null) {
                throw new IOException("Could not find decoder");
            }
            decCtx = avcodec.avcodec_alloc_context3(decoder);
            if (avcodec.avcodec_parameters_to_context(decCtx, ifCtx.streams(stream).codecpar()) < 0) {
                throw new IOException("Could not copy codec parameters to decoder context");
            }
            if (avcodec.avcodec_open2(decCtx, decoder, (AVDictionary) null) < 0) {
                throw new IOException("Could not open decoder");
            }
            src = avutil.av_frame_alloc();
            dst = avutil.av_frame_alloc();
            if (src == null || dst == null) {
                throw new IOException("Could not allocate frame");
            }
            pkt = avcodec.av_packet_alloc();
            boolean gotFrame = false;
            while (!gotFrame) {
                int ret2 = avformat.av_read_frame(ifCtx, pkt);
                if (ret2 < 0) {
                    break;
                }
                if (pkt.stream_index() != stream) {
                    avcodec.av_packet_unref(pkt);
                    continue;
                }
                int ret = avcodec.avcodec_send_packet(decCtx, pkt);
                if (ret < 0) {
                    throw new IOException("Could not send packet to decoder");
                }
                avcodec.av_packet_unref(pkt);
                while (ret >= 0) {
                    ret = avcodec.avcodec_receive_frame(decCtx, src);
                    if (ret == avutil.AVERROR_EAGAIN() || ret == avutil.AVERROR_EOF()) {
                        break;
                    } else if (ret < 0) {
                        throw new IOException("Could not receive frame from decoder");
                    }
                    gotFrame = true;
                    break;
                }
            }
            if (!gotFrame) {
                throw new IOException("Could not read frame");
            }
            dst.width(400);
            dst.height(400);
            dst.format(avutil.AV_PIX_FMT_YUVA420P);
            swsCtx = swscale.sws_getContext(
                    src.width(), src.height(), src.format(),
                    dst.width(), dst.height(), dst.format(),
                    swscale.SWS_BICUBIC, null, null, (double[]) null);
            if (swsCtx == null) {
                throw new IOException("Could not initialize the conversion context");
            }
            if (swscale.sws_scale_frame(swsCtx, dst, src) < 0) {
                throw new IOException("Error while converting");
            }
            AVCodec encoder = avcodec.avcodec_find_encoder_by_name("libwebp");
            if (encoder == null) {
                throw new IOException("Could not find encoder");
            }
            encCtx = avcodec.avcodec_alloc_context3(encoder);
            if (encCtx == null) {
                throw new IOException("Could not allocate the encoder context");
            }
            encCtx.time_base().num(1);
            encCtx.time_base().den(1);
            encCtx.width(dst.width());
            encCtx.height(dst.height());
            encCtx.pix_fmt(dst.format());
            if (avcodec.avcodec_open2(encCtx, encoder, (AVDictionary) null) < 0) {
                throw new IOException("Could not open encoder");
            }
            if (avcodec.avcodec_send_frame(encCtx, dst) < 0) {
                throw new IOException("Error while sending a frame to the encoder");
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (avcodec.avcodec_receive_packet(encCtx, pkt) >= 0) {
                BytePointer d = pkt.data();
                byte[] bytes = new byte[pkt.size()];
                d.get(bytes);
                baos.write(bytes, 0, bytes.length);
            }
            // flush packet
            if (avcodec.avcodec_send_frame(encCtx, null) < 0) {
                throw new IOException("Error while sending a frame to the encoder");
            }
            while (avcodec.avcodec_receive_packet(encCtx, pkt) >= 0) {
                BytePointer d = pkt.data();
                byte[] bytes = new byte[pkt.size()];
                d.get(bytes);
                baos.write(bytes, 0, bytes.length);
            }
            if (baos.size() == 0) {
                throw new IOException("Could not encode");
            }
            return baos.toByteArray();
        } finally {
            if (encCtx != null) {
                avcodec.avcodec_free_context(encCtx);
            }
            if (swsCtx != null) {
                swscale.sws_freeContext(swsCtx);
            }
            if (pkt != null) {
                avcodec.av_packet_free(pkt);
            }
            if (dst != null) {
                avutil.av_frame_free(dst);
            }
            if (src != null) {
                avutil.av_frame_free(src);
            }
            if (decCtx != null) {
                avcodec.avcodec_free_context(decCtx);
            }
            if (ifCtx != null) {
                avformat.avformat_free_context(ifCtx);
            }
            if (pb != null) {
                /* note: the internal buffer could have changed, and be != avio_ctx_buffer */
                avutil.av_free(pb.buffer());
                pb.buffer(null);
                avformat.avio_context_free(pb);
            }
            if (mem != null) {
                avutil.av_free(mem);
                mem.close();
            }
            if (data != null) {
                data.close();
            }
        }
    }

    private static final class ReadPointer extends AVIOContext.Read_packet_Pointer_BytePointer_int {
        public static final ReadPointer INSTANCE = new ReadPointer();

        @Override
        public int call(Pointer opaque, BytePointer buf, int buf_size) {
            BytePointer p = new BytePointer(opaque);
            try {
                long pos = p.getLong(0);
                long count = p.getLong(8);
                if (pos >= count) {
                    return avutil.AVERROR_EOF();
                }
                long avail = count - pos;
                if (buf_size > avail) {
                    buf_size = (int) avail;
                }
                if (buf_size <= 0) {
                    return 0;
                }
                Pointer.memcpy(buf, p.getPointer(16 + pos), buf_size);
                p.putLong(0, pos + buf_size);
                return buf_size;
            } finally {
                p.close();
            }
        }
    }

    private static final class SeekPointer extends AVIOContext.Seek_Pointer_long_int {
        public static final SeekPointer INSTANCE = new SeekPointer();

        public long call(Pointer opaque, long offset, int whence) {
            BytePointer p = new BytePointer(opaque);
            try {
                long pos = p.getLong(0);
                long size = p.getLong(8);
                if ((whence & avformat.AVSEEK_SIZE) != 0) {
                    return size;
                }
                switch (whence) {
                    case 0 -> pos = offset; // SEEK_SET
                    case 1 -> pos = pos + offset; // SEEK_CUR
                    case 2 -> pos = capacity + offset; // SEEK_END
                }
                if (offset < 0 || offset > size) {
                    return avutil.AVERROR_EOF();
                }
                p.putLong(0, pos);
                return 0;
            } finally {
                p.close();
            }
        }
    }
}
