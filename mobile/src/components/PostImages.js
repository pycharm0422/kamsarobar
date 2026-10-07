import { Image } from 'expo-image';
import { useState } from 'react';
import { Dimensions, FlatList, Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { imageUrl } from '../api';

/** One photo is shown whole at its own shape; several are shown as tiles (cropped, never stretched). Tap for full screen. */
export default function PostImages({ images }) {
  const [open, setOpen] = useState(null);
  if (!images?.length) return null;
  const single = images.length === 1;
  const ratio = single && images[0].width && images[0].height ? images[0].width / images[0].height : 4 / 3;

  return (
    <>
      {single ? (
        <Pressable onPress={() => setOpen(0)} accessibilityLabel="Open photo">
          <Image source={imageUrl(images[0])} style={[styles.single, { aspectRatio: Math.max(ratio, 0.6) }]} contentFit="contain" transition={150} />
        </Pressable>
      ) : (
        <View style={styles.grid}>
          {images.slice(0, 4).map((img, i) => (
            <Pressable key={img.id} style={styles.tile} onPress={() => setOpen(i)} accessibilityLabel="Open photo">
              <Image source={imageUrl(img)} style={StyleSheet.absoluteFill} contentFit="cover" transition={150} />
              {i === 3 && images.length > 4 ? (
                <View style={styles.more}><Text style={styles.moreText}>+{images.length - 4}</Text></View>
              ) : null}
            </Pressable>
          ))}
        </View>
      )}
      <Modal visible={open !== null} transparent animationType="fade" onRequestClose={() => setOpen(null)}>
        <View style={styles.viewer}>
          <FlatList
            data={images}
            horizontal
            pagingEnabled
            initialScrollIndex={open ?? 0}
            getItemLayout={(_, index) => ({ length: SCREEN.width, offset: SCREEN.width * index, index })}
            keyExtractor={(img) => img.id}
            renderItem={({ item }) => (
              <Image source={imageUrl(item)} style={{ width: SCREEN.width, height: SCREEN.height }} contentFit="contain" />
            )}
          />
          <Pressable style={styles.close} onPress={() => setOpen(null)} accessibilityLabel="Close"><Text style={styles.closeText}>×</Text></Pressable>
        </View>
      </Modal>
    </>
  );
}

const SCREEN = Dimensions.get('window');

const styles = StyleSheet.create({
  single: { width: '100%', maxHeight: 520, borderRadius: 12, backgroundColor: '#eef0ec', marginVertical: 8 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 4, marginVertical: 8, borderRadius: 12, overflow: 'hidden' },
  tile: { width: '49.3%', aspectRatio: 4 / 3, backgroundColor: '#eef0ec' },
  more: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,0.45)', alignItems: 'center', justifyContent: 'center' },
  moreText: { color: '#fff', fontSize: 24, fontWeight: '800' },
  viewer: { flex: 1, backgroundColor: '#000' },
  close: { position: 'absolute', top: 44, right: 18, padding: 8 },
  closeText: { color: '#fff', fontSize: 36 },
});
